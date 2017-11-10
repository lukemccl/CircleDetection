import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;

public class CircleDetection {
    private static BufferedImage grey;
    private static int[][] sobelX;
    private static int[][] sobelY;
    private static double[][] sobelTotal;
    private static String path = System.getProperty("user.dir");
    private static int maxX = 0;
    private static int maxY = 0;
    private static int maxR = 0;

    public static void main(String[] args) throws Exception{
        File originalFile = new File(args[0]);
        toGrayScale(originalFile);
        edgeDetection();
        

        BufferedImage img = new BufferedImage(grey.getWidth(), grey.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for(int i = 0; i< grey.getWidth(); i++){
            for(int j = 0; j<grey.getHeight(); j++){
                img.setRGB(i, j, sobelX[i][j]);
            }
        }
        File output = new File(path+"\\result\\testX.png");
        ImageIO.write(img, "png", output);


        BufferedImage ing = new BufferedImage(grey.getWidth(), grey.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for(int i = 0; i< grey.getWidth(); i++){
            for(int j = 0; j<grey.getHeight(); j++){
                ing.setRGB(i, j, sobelY[i][j]);
            }
        }
        File out= new File(path+"\\result\\testY.png");
        ImageIO.write(ing, "png", out);

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
                int rgb = new Color((int)map(sobelTotal[i][j], 0,max,0,255),
                        (int)map(sobelTotal[i][j], 0,max,0,255),
                        (int)map(sobelTotal[i][j], 0,max,0,255), 255).getRGB();
                total.setRGB(i,j,rgb);
            }
        }
        total = changeBrightness(20.0f, total);
        File out2 = new File(path+"\\result\\total.png");
        ImageIO.write(total, "png", out2);
        
        BufferedImage totalColour = new BufferedImage(grey.getWidth(), grey.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < grey.getWidth(); i++) {
        	for(int j = 0; j < grey.getHeight(); j++) {
        		Color c1 = new Color(0,255,0);
        		if(sobelTotal[i][j] > 250) {
        			totalColour.setRGB(i, j, c1.getRGB());
        		}
        	}
        }
        File outtt = new File(path+"\\result\\totalGreen.png");
        ImageIO.write(totalColour, "png", outtt);
        
        
        BufferedImage totalCircles;
        circleDetection(total);
        totalCircles = total;
        for(int t = 0; t<= 360; t++) {
        	Color c2 = new Color(0, 255, 0);
			double a =  maxX - maxR * Math.cos(t * Math.PI / 180);
			double b =  maxY - maxR * Math.sin(t * Math.PI / 180);
			totalCircles.setRGB((int) a, (int) b, c2.getRGB());
        }
        File outttt = new File(path+"\\result\\totalCircles.png");
        ImageIO.write(totalCircles, "png", outttt);
    }

    private static double map(double valueCoord1,
                             double startCoord1, double endCoord1,
                             double startCoord2, double endCoord2) {


        double ratio = (endCoord2 - startCoord2) / (endCoord1 - startCoord1);
        return ratio * (valueCoord1 - startCoord1) + startCoord2;
    }

    private static void toGrayScale(File f) throws Exception{
        BufferedImage img = ImageIO.read(f);
        grey = new BufferedImage(img.getWidth(), img.getHeight(), img.TYPE_BYTE_GRAY);
        grey.getGraphics().drawImage(img, 0 , 0, null);
    }

    private static void edgeDetection(){
        calcSobelX();
        calcSobelY();
        combineSobel();
    }

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

    private static void combineSobel(){
        sobelTotal = new double[grey.getWidth()][grey.getHeight()];
        for(int i = 0; i <grey.getWidth(); i++){
            for(int j = 0; j<grey.getHeight(); j++){
                sobelTotal[i][j] = Math.round(Math.sqrt(Math.pow((double)sobelX[i][j],2) + Math.pow((double)sobelY[i][j],2)));
            }
        }
    }
    
    private static void circleDetection(BufferedImage image) throws Exception {
        int radius = 50;
        int[][][] A = new int[image.getWidth()][image.getHeight()][radius];

        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int rad = 0; rad < radius; rad++) {
            for (int x = 0; x < newImage.getWidth(); x++) {
                for (int y = 0; y < newImage.getHeight(); y++) {
                    if (sobelTotal[x][y] > 250) {
                        for (int t = 0; t <= 360; t++) {
                            Integer a = (int) Math.floor(x - rad * Math.cos(t * Math.PI / 180));
                            Integer b = (int) Math.floor(y - rad * Math.sin(t * Math.PI / 180));
                            if (!((0 > a || a > newImage.getWidth() - 1) || (0 > b || b > newImage.getHeight() - 1))) {
                                Color c = new Color(newImage.getRGB(a, b));
                                Color c1;
                                if(c.getBlue()==255){
                                    c1 = new Color( c.getRed(), c.getGreen()+1,0);
                                }else if(c.getGreen()==255){
                                    c1 = new Color(c.getRed()+1,0, c.getBlue());
                                }else{
                                    c1 = new Color(c.getRed(), c.getGreen(), c.getBlue()+1);
                                }
                                newImage.setRGB(a, b, c1.getRGB());
                                if(!(a.equals(x) && b.equals(y))){
                                    A[a][b][rad] += 1;
                                }
                            }
                        }
                    }
                }
            }
        }
        int max = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int r = 5; r < radius; r++) {
                    if (A[x][y][r] > max) {
                        max = A[x][y][r];
                        maxX = x;
                        maxY = y;
                        maxR = r;
                    }
                }
            }
        }
        System.out.println(maxX + " " + maxY + " " + maxR);
        File newfile = new File(path+"\\result\\newimage.png");
        ImageIO.write(newImage, "png", newfile);
    }

    private static BufferedImage changeBrightness(float brightenFactor, BufferedImage image){
        RescaleOp op = new RescaleOp(brightenFactor, 0, null);
        image = op.filter(image, image);
        return image;
    }
}
