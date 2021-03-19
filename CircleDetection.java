import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.util.*;
import java.util.List;

public class CircleDetection {
    private static BufferedImage grey;
    private static int[][] sobelX;
    private static int[][] sobelY;
    private static double[][] sobelTotal;
    private static String path = System.getProperty("user.dir");
    private static int threshold = 150;
    private static int drawnCircles = 1;
    private static int minRad = 10;

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

        //arguments are [File Path] [Num Drawn Circles] [Sobel Threshold] [Minimum radius circle]
        if(args.length>1){
            if(args.length>2){
                if(args.length>3){
                    minRad = Integer.parseInt(args[3]);
                }
                threshold = Integer.parseInt(args[2]);
            }
            drawnCircles = Integer.parseInt(args[1]);
        }

        //runs the required processes for circle detection, ie sobel edge detection
        toGrayScale(originalFile);
        edgeDetection();

        BufferedImage total = new BufferedImage(grey.getWidth(), grey.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        double max = 0;
        for(int i = 0; i< grey.getWidth(); i++){
            for(int j = 0; j<grey.getHeight(); j++){
                if(sobelTotal[i][j]>max){
                    max = sobelTotal[i][j];
                }
            }
        }
        for(int i = 0; i< grey.getWidth(); i++){
            for(int j = 0; j<grey.getHeight(); j++){
                //maps every pixel to a grayscale value between 0 and 255 from between 0 and the max value in sobelTotal
                int rgb = new Color((int)map(sobelTotal[i][j], 0,max,0,255),
                        (int)map(sobelTotal[i][j], 0,max,0,255),
                        (int)map(sobelTotal[i][j], 0,max,0,255), 255).getRGB();
                total.setRGB(i,j,rgb);
            }
        }
        total = changeBrightness(20.0f, total);

        createSobelImages(total);

        //runs circle detection and outputs an image of the original with the circle detected superimposed in red
        BufferedImage totalCircles = new BufferedImage(grey.getWidth(), grey.getHeight(), BufferedImage.TYPE_INT_ARGB);
        List<CircleHit> hits = circleDetection(total);
        Collections.sort(hits, Collections.reverseOrder());

        total = changeBrightness(0.5f, total);
        totalCircles.getGraphics().drawImage(total, 0, 0, null);
        Graphics2D g = totalCircles.createGraphics();
        g.setColor(Color.RED);
        for(int circles = 0; circles<drawnCircles; circles++){
            CircleHit toDraw = hits.get(circles);
            double a =  toDraw.x - toDraw.r * Math.cos(0 * Math.PI / 180);
            double b =  toDraw.y - toDraw.r * Math.sin(90 * Math.PI / 180);
            g.drawOval((int)a,(int)b,2*toDraw.r,2*toDraw.r);
        }

        File outfinal = new File(path+"\\result\\detectedCircles.png");
        ImageIO.write(totalCircles, "png", outfinal);
    }

    private static void createSobelImages(BufferedImage total) throws Exception {

        //creates and outputs images for the sobel sweep in x and y direction, and the combined sobel output
        BufferedImage img = new BufferedImage(grey.getWidth(), grey.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for(int i = 0; i< grey.getWidth(); i++){
            for(int j = 0; j<grey.getHeight(); j++){
                img.setRGB(i, j, sobelX[i][j]);
            }
        }
        File outX = new File(path+"\\result\\sobelX.png");
        ImageIO.write(img, "png", outX);

        BufferedImage ing = new BufferedImage(grey.getWidth(), grey.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for(int i = 0; i< grey.getWidth(); i++){
            for(int j = 0; j<grey.getHeight(); j++){
                ing.setRGB(i, j, sobelY[i][j]);
            }
        }
        File outY = new File(path+"\\result\\sobelY.png");
        ImageIO.write(ing, "png", outY);

        //outputs an image showing every pixel that is above the threshold
        BufferedImage totalColour = new BufferedImage(grey.getWidth(), grey.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < grey.getWidth(); i++) {
            for(int j = 0; j < grey.getHeight(); j++) {
                Color c1 = new Color(0,255,0);
                if(sobelTotal[i][j] > threshold) {
                    totalColour.setRGB(i, j, c1.getRGB());
                }
            }
        }
        File outThreshold = new File(path+"\\result\\sobelAboveThreshold.png");
        ImageIO.write(totalColour, "png", outThreshold);

        File out2 = new File(path+"\\result\\sobelTotal.png");
        ImageIO.write(total, "png", out2);
    }

    //maps the given value between startCoord1 and endCoord1 to a value between startCoord2 and endCoord2
    private static double map(double valueCoord1,
                             double startCoord1, double endCoord1,
                             double startCoord2, double endCoord2) {


        double ratio = (endCoord2 - startCoord2) / (endCoord1 - startCoord1);
        return ratio * (valueCoord1 - startCoord1) + startCoord2;
    }

    //converts given file into a grayscale image
    private static void toGrayScale(File f) throws Exception{
        BufferedImage img = ImageIO.read(f);
        grey = new BufferedImage(img.getWidth(), img.getHeight(), img.TYPE_BYTE_GRAY);
        grey.getGraphics().drawImage(img, 0 , 0, null);
    }

    //runs all the functions required to complete edge detection
    private static void edgeDetection(){
        calcSobelX();
        calcSobelY();
        combineSobel();
    }

    //performs the horizontal sobel sweep, sets up the matrix using a 3x3 array and runs through
    //every pixel to calculate the sobel result
    private static void calcSobelX(){
        sobelX = new int[grey.getWidth()][grey.getHeight()];
        int[][] base = new int[3][3];
        base[0][0] = -1;
        base[1][0] = -2;
        base[2][0] = -1;
        base[0][1] = 0;
        base[1][1] = 0;
        base[2][1] = 0;
        base[0][2] = 1;
        base[1][2] = 2;
        base[2][2] = 1;


        for(int i = 0; i<grey.getWidth(); i++){
            for(int j = 0; j<grey.getHeight();j++){
                sobelX[i][j] = getSobelResult(i,j,base);
            }
        }
    }

    //performs the vertical sobel sweep, sets up the matrix using a 3x3 array and runs through
    //every pixel to calculate the sobel result
    private static void calcSobelY(){
        sobelY = new int[grey.getWidth()][grey.getHeight()];
        int[][] base = new int[3][3];
        base[0][0] = -1;
        base[0][1] = -2;
        base[0][2] = -1;
        base[1][0] = 0;
        base[1][1] = 0;
        base[1][2] = 0;
        base[2][0] = 1;
        base[2][1] = 2;
        base[2][2] = 1;


        for(int i = 0; i<grey.getWidth(); i++){
            for(int j = 0; j<grey.getHeight();j++){
                sobelY[i][j] = getSobelResult(i,j,base);
            }
        }
    }

    //a series of if statements to account for any edge cases to ensure no errors, calculates
    //the sobel result for any pixel and kernel
    private static int getSobelResult(int x, int y, int[][] base){
        int result = 0;
        if(x==0 && y ==0){
            for(int i = 0; i <= 1; i++){
                for(int j = 0; j <= 1; j++){
                    result += new Color(grey.getRGB(x+i,y+j)).getRed()*base[j+1][i+1];
                }
            }
        }else if(x==0 && y==grey.getHeight()-1){
            for(int i = 0; i <= 1; i++){
                for(int j = -1; j <= 0; j++){
                    result += new Color(grey.getRGB(x+i,y+j)).getRed()*base[j+1][i+1];
                }
            }
        }else if(x==0 && y !=0){
            for (int i = 0; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    result += new Color(grey.getRGB(x + i, y + j)).getRed()* base[j + 1][i + 1];
                }
            }

        }else if(x==grey.getWidth()-1 && y ==0){
            for(int i = -1; i <= 0; i++){
                for(int j = 0; j <= 1; j++){
                    result += new Color(grey.getRGB(x+i,y+j)).getRed()*base[j+1][i+1];
                }
            }
        }else if(x!=0 && y ==0){
            for(int i = -1; i <= 1; i++){
                for(int j = 0; j <= 1; j++){
                    result += new Color(grey.getRGB(x+i,y+j)).getRed()*base[j+1][i+1];
                }
            }
        }else if(x==grey.getWidth()-1 && y==grey.getHeight()-1){
            for(int i = -1; i <= 0; i++){
                for(int j = -1; j <= 0; j++){
                    result += new Color(grey.getRGB(x+i,y+j)).getRed()*base[j+1][i+1];
                }
            }
        }else if(x==grey.getWidth()-1 && y!=grey.getHeight()-1){
            for(int i = -1; i <= 0; i++){
                for(int j = -1; j <= 1; j++){
                    result += new Color(grey.getRGB(x+i,y+j)).getRed()*base[j+1][i+1];
                }
            }
        }else if(x!=grey.getWidth()-1 && y==grey.getHeight()-1){
            for(int i = -1; i <= 1; i++){
                for(int j = -1; j <= 0; j++){
                    result += new Color(grey.getRGB(x+i,y+j)).getRed()*base[j+1][i+1];
                }
            }
        }else{
            for(int i = -1; i <= 1; i++){
                for(int j = -1; j <= 1; j++){
                    result += new Color(grey.getRGB(x+i,y+j)).getRed()*base[j+1][i+1];
                }
            }
        }

        return result;
    }

    //performs the algorithm to combine the horizontal sweep and vertical sweep
    private static void combineSobel(){
        sobelTotal = new double[grey.getWidth()][grey.getHeight()];
        //indexing removes bright spots from perimeter of image
        for(int i = 1; i <grey.getWidth()-1; i++){
            for(int j = 1; j<grey.getHeight()-1; j++){
                sobelTotal[i][j] = Math.round(Math.sqrt(Math.pow((double)sobelX[i][j],2) + Math.pow((double)sobelY[i][j],2)));
            }
        }
    }
    
    private static List<CircleHit> circleDetection(BufferedImage image) throws Exception {
        //sets the radius relative to 1/6 of the smallest side of the image, helps reduce space taken in memory during
        //runtime
        int radius;
        if (image.getHeight() < image.getWidth()) {
            radius = image.getHeight() / 6;
        } else {
            radius = image.getWidth() / 6;
        }
        //sets a 3D space array of ints to hold 'hits' in x, y, and r planes
        int[][][] A = new int[image.getWidth()][image.getHeight()][radius];
        int maxCol = 0;

        BufferedImage houghSpace = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < houghSpace.getWidth(); x++) {
            for (int y = 0; y < houghSpace.getHeight(); y++) {
                //if the given pixel is above the threshold, a circle will be drawn at radius rad around it and if it
                //is a valid coordinate it will be accumulated in the A array and plotted in the pointSpace image
                if (sobelTotal[x][y] > threshold) {
                    for (int rad = 1; rad < radius; rad++) {
                        for (int t = 0; t <= 360; t++) {
                            Integer a = (int) Math.floor(x - rad * Math.cos(t * Math.PI / 180));
                            Integer b = (int) Math.floor(y - rad * Math.sin(t * Math.PI / 180));

                            //if a or b is outside the bounds of the image ignore
                            if (!((0 > a || a > houghSpace.getWidth() - 1) || (0 > b || b > houghSpace.getHeight() - 1))) {
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
        List<CircleHit> AList = new ArrayList<>();
        for (int x = 0; x < houghSpace.getWidth(); x++) {
            for (int y = 0; y < houghSpace.getHeight(); y++) {
                for (int rad = minRad; rad < radius; rad++) {
                    AList.add(new CircleHit(x,y,rad,A[x][y][rad]));

                    int colour = (int) Math.floor(map(A[x][y][rad],0,maxCol, 0, 255));
                    houghSpace.setRGB(x, y, new Color(255, 255, 255, colour).getRGB());
                }
            }
        }
        //outputs the pointSpace image
        File newfile = new File(path + "\\result\\houghSpace.png");
        ImageIO.write(houghSpace, "png", newfile);

        return AList;
    }

    private static Color incrementColourValue(Color c){
        Color c1;
        if (c.getBlue() == 255) {
            c1 = new Color(c.getRed(), c.getGreen() + 1, 0);
        } else if (c.getGreen() == 255) {
            c1 = new Color(c.getRed() + 1, 0, c.getBlue());
        } else {
            c1 = new Color(c.getRed(), c.getGreen(), c.getBlue() + 1);
        }
        return c1;
    }

    //changes the brightness of an image by the factor given
    private static BufferedImage changeBrightness(float brightenFactor, BufferedImage image){
        RescaleOp op = new RescaleOp(brightenFactor, 0, null);
        image = op.filter(image, image);
        return image;
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

    public CircleHit incAHit(){
        AhitMag+=1;
        return this;
    }

    @Override
    public int compareTo(CircleHit o) {
        return Integer.compare(getAhitMag(), o.getAhitMag());
    }
}
