import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

public class CircleDetection {

    private static String path = System.getProperty("user.dir");
    private static int imgHeight;
    private static int imgWidth;
    private static int threshold = 150;
    private static int minRad = 10;
    private static int maxRad = 0;

    public static void main(String[] args) throws Exception{
        //ensures all the file systems required are in order
        File originalFile = new File(args[0]);
        File originalDirectory = new File(path+"\\result");
        if(!(originalDirectory.exists() && originalDirectory.isDirectory())){
            try{
                originalDirectory.mkdir();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //arguments are [File Path] [Num Drawn Circles] [Sobel Threshold] [Minimum radius circle] [Max radius circle]
        if(args.length>1){
            if(args.length>2){
                if(args.length>3){
                    minRad = Integer.parseInt(args[3]);
                    if(args.length>4){
                        maxRad = Integer.parseInt(args[4]);
                    }
                }
                threshold = Integer.parseInt(args[2]);
            }
            Output.drawnCircles = Integer.parseInt(args[1]);
        }

        //greyscaling
        BufferedImage grey = toGrayScale(originalFile);

        Output.imgWidth = imgWidth = grey.getWidth();
        Output.imgHeight = imgHeight = grey.getHeight();

        //convert image to array
        double[][] imgArray = new double[imgWidth][imgHeight];
        for (int x = 0; x < imgWidth; x++) {
            for (int y = 0; y < imgHeight; y++) {
                imgArray[x][y] = new Color(grey.getRGB(x,y)).getRed();
            }
        }

        //gauss blur??
        imgArray = blur(imgArray);

        //sobel edge detection
        double[][] sobelTotal = edgeDetection(imgArray);

        //runs circle detection
        List<CircleHit> hits = circleDetection(sobelTotal);
        Collections.sort(hits, Collections.reverseOrder());

        //outputs an image of the original with the circle detected superimposed in red
        Output.superimposeCircles(hits, sobelTotal);
    }

    private static double[][] blur(double[][] imgArray) {
        double sigma = 1;    //must be >0
        int width = 15;      //must be odd

        int pad = (int) Math.floor(width/2);
        double[][] base = new double[width][width];

        //build gaussian kernel
        double scaling = 1/(2*Math.PI* Math.pow(sigma, 2));

        for(int x = -pad; x<=pad; x++){
            for(int y = -pad; y<= pad; y++){
                double components = (Math.pow(x,2) + Math.pow(y,2));
                double exp = -(components/(2*Math.pow(sigma,2)));
                base[x+pad][y+pad] = scaling * Math.exp(exp);
            }
        }

        return KernelSweep(imgArray, base);
    }

    //converts given file into a grayscale image
    private static BufferedImage toGrayScale(File f) throws Exception{
        BufferedImage img = ImageIO.read(f);
        BufferedImage grey = new BufferedImage(img.getWidth(), img.getHeight(), img.TYPE_BYTE_GRAY);
        grey.getGraphics().drawImage(img, 0 , 0, null);

        return grey;
    }

    //runs all the functions required to complete edge detection
    private static double[][] edgeDetection(double[][] grey) throws Exception{
        double[][] sobelX = calcSobelX(grey);
        double[][] sobelY = calcSobelY(grey);
        double[][] sobelTotal = combineSobel(sobelX, sobelY);

        Output.writeImage(sobelX, "\\result\\sobelX.png");
        Output.writeImage(sobelY, "\\result\\sobelY.png");
        Output.writeImage(Output.scaledSobelResult(sobelTotal), "\\result\\sobelTotal.png");
        Output.writeImage(sobelTotal, "\\result\\sobelAboveThreshold.png", threshold);

        return sobelTotal;
    }

    //performs the horizontal sobel sweep, sets up the matrix using a 3x3 array and runs through
    //every pixel to calculate the sobel result
    private static double[][] calcSobelX(double[][] imgArray){
        double[][] base = new double[3][3];
        base[0][0] = -1;
        base[1][0] = -2;
        base[2][0] = -1;
        base[0][1] = 0;
        base[1][1] = 0;
        base[2][1] = 0;
        base[0][2] = 1;
        base[1][2] = 2;
        base[2][2] = 1;

        return KernelSweep(imgArray, base);
    }

    //performs the vertical sobel sweep, sets up the matrix using a 3x3 array and runs through
    //every pixel to calculate the sobel result
    private static double[][] calcSobelY(double[][] imgArray){
        double[][] base = new double[3][3];
        base[0][0] = -1;
        base[0][1] = -2;
        base[0][2] = -1;
        base[1][0] = 0;
        base[1][1] = 0;
        base[1][2] = 0;
        base[2][0] = 1;
        base[2][1] = 2;
        base[2][2] = 1;

        return KernelSweep(imgArray, base);
    }

    private static double[][] KernelSweep(double[][] base, double[][] kernel){
        int paddingX = (int) Math.floor((double) kernel.length/2);
        int paddingY = (int) Math.floor((double) kernel[0].length/2);

        double[][] result = new double[imgWidth][imgHeight];
        double[][] padded = new double[imgWidth+paddingX][imgHeight+paddingY];

        //pad base into padded
        for(int x = paddingX; x<imgWidth; x++){
            for(int y = paddingY; y<imgHeight;y++){
                padded[x][y] = base[x-paddingX][y-paddingY];
            }
        }

        //sweep over every value in padded
        for(int x = paddingX; x<imgWidth; x++){
            for(int y = paddingY; y<imgHeight;y++){

                //sweep kernel through point
                for(int i = -paddingX; i <= paddingX; i++){
                    for(int j = -paddingY; j <= paddingY; j++){
                        result[x-paddingX][y-paddingY] += padded[x+i][y+j]*kernel[j+paddingY][i+paddingX];
                    }
                }
            }
        }

        return result;
    }

    //performs the algorithm to combine the horizontal sweep and vertical sweep
    private static double[][] combineSobel(double[][] sobelX, double[][] sobelY){
        double[][] sobelTotal = new double[imgWidth][imgHeight];

        //indexing removes bright spots from perimeter of image
        for(int i = 1; i <imgWidth-1; i++){
            for(int j = 1; j<imgHeight-1; j++){
                sobelTotal[i][j] = Math.round(Math.sqrt(Math.pow(sobelX[i][j],2) + Math.pow(sobelY[i][j],2)));
            }
        }
        return sobelTotal;
    }
    
    private static List<CircleHit> circleDetection(double[][] sobelTotal) throws Exception {
        //sets the max radius of a circle, default 0 == min of height or width (biggest circle able to be displayed)
        int radius = maxRad == 0 ? Integer.min(imgHeight, imgWidth) : maxRad;

        //sets a 3D space array of ints to hold 'hits' in x, y, and r planes
        int[][][] A = new int[imgWidth][imgHeight][radius];
        int maxCol = 0;

        for (int x = 0; x < imgWidth; x++) {
            for (int y = 0; y < imgHeight; y++) {
                //if the given pixel is above the threshold, a circle will be drawn at radius rad around it and if it
                //is a valid coordinate it will be accumulated in the A array and plotted in the pointSpace image
                if (sobelTotal[x][y] > threshold) {
                    for (int rad = 1; rad < radius; rad++) {
                        for (int t = 0; t <= 360; t++) {
                            Integer a = (int) Math.floor(x - rad * Math.cos(t * Math.PI / 180));
                            Integer b = (int) Math.floor(y - rad * Math.sin(t * Math.PI / 180));

                            //if a or b is outside the bounds of the image ignore
                            if (!((0 > a || a > imgWidth - 1) || (0 > b || b > imgHeight - 1))) {
                                A[a][b][rad] += 1;
                                if(A[a][b][rad]>maxCol){
                                    maxCol = A[a][b][rad];
                                }
                            }
                        }
                    }
                }
            }
        }
        int[][] houghSpace = new int[imgWidth][imgHeight];
        List<CircleHit> AList = new ArrayList<>();
        for (int x = 0; x < imgWidth; x++) {
            for (int y = 0; y < imgHeight; y++) {
                for (int rad = minRad; rad < radius; rad++) {
                    AList.add(new CircleHit(x,y,rad,A[x][y][rad]));

                    houghSpace[x][y] = (int) Math.floor(Output.map(A[x][y][rad],0,maxCol, 0, 255));
                }
            }
        }

        Output.writeImage(houghSpace, "\\result\\houghSpace.png");

        return AList;
    }
}

class CircleHit implements Comparable<CircleHit>{

    short x;
    short y;
    short r;
    short AhitMag;

    public CircleHit(int x, int y, int r){
        this.x = (short) x;
        this.y = (short) y;
        this.r = (short) r;
        this.AhitMag = 1;
    }

    public CircleHit(int x, int y, int r, int A){
        this.x = (short) x;
        this.y = (short) y;
        this.r = (short) r;
        this.AhitMag = (short) A;
    }

    public Short getAhitMag() {
        return AhitMag;
    }

    @Override
    public int compareTo(CircleHit o) {
        return Integer.compare(getAhitMag(), o.getAhitMag());
    }
}
