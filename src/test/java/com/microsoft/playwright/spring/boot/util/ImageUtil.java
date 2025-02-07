package com.microsoft.playwright.spring.boot.util;

import com.microsoft.playwright.spring.boot.enums.PDPageSize;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Coordinate;
import net.coobird.thumbnailator.geometry.Position;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.resizers.configurations.AlphaInterpolation;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;
import net.coobird.thumbnailator.resizers.configurations.Dithering;
import net.coobird.thumbnailator.resizers.configurations.ScalingMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Slf4j
public class ImageUtil {

    public static BufferedImage WHITE_A4 = ImageUtil.getWhiteImage(Float.valueOf(PDPageSize.A4.getRectangle().getWidth()).intValue(), Float.valueOf(PDPageSize.A4.getRectangle().getHeight()).intValue());
    public static long WHITE_A4_SIZE = ImageUtil.getImageFileSize(WHITE_A4, "png");

    public static long getImageFileSize(BufferedImage image, String formatName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
            ImageIO.write(image, formatName, baos);
            baos.flush();
            return baos.size();
        } catch (IOException e) {
            log.error("getImageFileSize error", e);
            return 0;
        }
    }

    public static BufferedImage getImageOnlyColor(int width, int height, Color color) {
        // Create a buffered image with the specified width and height
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Get the graphics object from the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();
        // Set the background color to white
        g2d.setColor(Color.WHITE);
        // Fill the entire image with the white background
        g2d.fillRect(0, 0, width, height);
        // Dispose the graphics object
        g2d.dispose();
        return bufferedImage;
    }

    public static BufferedImage getWhiteImage(int width, int height) {
        return getImageOnlyColor(width, height, Color.WHITE);
    }

    public static boolean isWhiteImage(BufferedImage image) {
        return isImageOnlyRgb(image, 0xFFFFFF);
    }

    public static boolean isImageOnlyRgb(BufferedImage image, int whiteRgb) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (image.getRGB(x, y) != whiteRgb) {
                    return false;
                }
            }
        }
        return true;
    }

    public static BufferedImage scaleToA3(BufferedImage image) throws IOException {
        return scaleToA3(image, 100d);
    }

    public static BufferedImage scaleToA3(BufferedImage image, Double quality) throws IOException {
        // 如果图片宽高大于A3纸张宽高，需要进行缩放处理
        if(image.getWidth() > PDRectangle.A3.getWidth() || image.getHeight() > PDRectangle.A3.getHeight()){
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
                // 计算缩放比例，使图像适应页面的大小
                float scaleFactor = Math.min(PDRectangle.A3.getWidth() / image.getWidth(), PDRectangle.A3.getWidth() / image.getHeight());
                // 计算缩放后的图像尺寸
                float scaledWidth = PDRectangle.A3.getWidth() / image.getWidth();
                float scaledHeight = PDRectangle.A3.getHeight() / image.getHeight();
                log.info("scaleFactor:{}, scaledWidth:{} , scaledHeight:{}  ", scaleFactor, scaledWidth, scaledHeight);
                return Thumbnails.of(image)
                        .scalingMode(ScalingMode.BICUBIC)
                        .scale(scaledWidth, scaledHeight)
                        .outputQuality(quality / 100f)
                        .asBufferedImage();
            }
        }
        return image;
    }

    public static BufferedImage scaleToA4(BufferedImage image) throws IOException {
        return scaleToA4(image, 100d);
    }

    public static BufferedImage scaleToA4(BufferedImage image, Double quality) throws IOException {
        if(image.getWidth() > PDRectangle.A4.getWidth() || image.getHeight() > PDRectangle.A4.getHeight()){
            // 计算缩放比例，使图像适应页面的大小
            float scaleFactor = Math.min(PDRectangle.A4.getWidth() / image.getWidth(), PDRectangle.A4.getWidth() / image.getHeight());
            // 计算缩放后的图像尺寸
            float scaledWidth = PDRectangle.A4.getWidth() / image.getWidth();
            float scaledHeight = PDRectangle.A4.getHeight() / image.getHeight();
            log.info("scaleFactor:{}, scaledWidth:{} , scaledHeight:{}  ", scaleFactor, scaledWidth, scaledHeight);
            return Thumbnails.of(image)
                    .alphaInterpolation(AlphaInterpolation.QUALITY)
                    .antialiasing(Antialiasing.OFF)
                    .dithering(Dithering.ENABLE)
                    .scale(scaledWidth, scaledHeight)
                    .outputQuality(1f)
                    .asBufferedImage();
        }
        return image;
    }

    /**

     * 按宽高进行缩放，此时有几个图片宽度与高度大小的逻辑处理

     */

    public static void zoomSize(File srcFile , File outFile , int width , int height){

        try {

            Thumbnails.of(srcFile).size(width, height).toFile(outFile);

            //也可将图片输出至OutputStream

            //是否使用等比缩放，keepAspectRatio(false)

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    /**

     * 将图片进行等比例缩放，0~~1之间的为缩小，大于1的为放大

     */

    public static void zoomScaling(File srcFile , File outFile , double scale){

        try {

            Thumbnails.of(srcFile).scale(scale).toFile(outFile);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    /**

     * 按角度旋转

     */

    public static void rotateAngle(File srcFile , File outFile , double rotate){

        try {

            Thumbnails.of(srcFile).scale(1).rotate(rotate).toFile(outFile);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    /**

     * 按坐标位置裁切图片，先按位置获取指定的图片范围，再按指定宽高存储

     */

    public static void corpRegion(File srcFile , File outFile , Positions postition , int width , int height , int x , int y){

        try {

            Thumbnails.of(srcFile).sourceRegion(postition, x, y).size(width, height).toFile(outFile);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    /**

     * 按坐标位置裁切图片，先按位置获取指定的图片范围，再按指定宽高存储

     * x1，x2为左上角的坐标点

     * y1，y2为以左上角为0,0坐标位置的相对坐标（或者x1+要裁切图片宽度=y1,y1+要裁切图片高度=y2的坐标）

     */

    public static void corpRegion(File srcFile , File outFile , int width , int height , int x1 , int y1 , int x2 , int y2){

        try {

            Thumbnails.of(srcFile).sourceRegion(x1, y1, x2, y2).size(width, height).keepAspectRatio(false).toFile(outFile);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    /**

     * 添加文本水印，也可添加图片水印

     */

    public static void watermark(File srcFile , File outFile , Position position , String text , double rotate , float opacity){

        //从原图中找出300x300的大小来显示水印文本

        BufferedImage bi = new BufferedImage(300, 300 ,BufferedImage.TYPE_INT_RGB);

        Graphics2D g = bi.createGraphics();

        //设置绘图区域透明

        bi = g.getDeviceConfiguration().createCompatibleImage(300, 300,Transparency.TRANSLUCENT);

        g.dispose();

        //字体、字体大小，透明度，旋转角度

        g = bi.createGraphics();

        g.setFont(new Font("微软雅黑", Font.BOLD, 26));

        char[] data = text.toCharArray();

        g.rotate(rotate);

        g.setColor(Color.RED);

        //设置文本显示坐标，以上述中的300x300位置为0,0点

        g.drawChars(data, 0, data.length, -70, 200);

        try {

            Thumbnails.of(srcFile).scale(1).watermark(position, bi, opacity).toFile(outFile);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    public static void main(String[] args) throws IOException {

        File srcFile = new File("D:/growth/output/707773105468432384/1.png");

        BufferedImage image = scaleToA3(ImageIO.read(srcFile));
        ImageIO.write(image, "png", new File("D:/growth/output/707773105468432384/1-1.png"));

        int width = image.getWidth();

        int height = image.getHeight();

        //原图大小为：600，600

        System.out.println("原图宽度为：" + width + " ，高度为 " + height);

        System.out.println("=========缩放图片=========");

        //按宽高缩小3倍，200x200

        zoomSize(srcFile, new File(srcFile.getParent() , "按宽高缩小3倍.jpg"), width/3, height/3);

        //按宽高放大1.5倍，900x900

        zoomSize(srcFile, new File(srcFile.getParent() , "按宽高放大1.5倍.jpg"), (int)(width*1.5), (int)(height*1.5));

        //按比例缩小3倍，600*0.3=180

        zoomScaling(srcFile, new File(srcFile.getParent() , "按比例缩小3倍.jpg"), 0.3);

        //按比例放大3倍，600*3=1800

        zoomScaling(srcFile, new File(srcFile.getParent() , "按比例放大3倍.jpg"), 3);

        System.out.println("=========旋转图片=========");

        rotateAngle(srcFile , new File(srcFile.getParent() , "旋转45度.jpg") , 45);

        rotateAngle(srcFile , new File(srcFile.getParent() , "旋转90度.jpg") , 90);

        rotateAngle(srcFile , new File(srcFile.getParent() , "旋转135度.jpg") , 135);

        System.out.println("=========裁切图片=========");

        corpRegion(srcFile , new File(srcFile.getParent() , "从正中间裁切400像素再缩小为200像素大小.jpg") , Positions.CENTER , 200 , 200 , 400 , 400);

        corpRegion(srcFile , new File(srcFile.getParent() , "从顶部中间裁切400像素再缩小为200像素大小.jpg") , Positions.TOP_CENTER , 200 , 200 , 400 , 400);

        corpRegion(srcFile, new File(srcFile.getParent() , "按自定义坐标裁切图片.jpg"), 350, 300, 150, 200, 350, 300);

        System.out.println("=========水印图片=========");

        //此功能由于项目中使用了此种方式，故代码比较多，功能相对比较多，但具体的代码，参数应该自行设置

        //水印可以同时添加多个，添加方式为：.watermark(顶部).watermark(中部).watermark(底部)

        watermark(srcFile, new File(srcFile.getParent() , "图片正中间水印旋转45度.jpg") , Positions.CENTER , "http://www.chendd.cn" , -0.5 , 0.5f);

        watermark(srcFile, new File(srcFile.getParent() , "自定义位置添加水印.jpg") , new Coordinate(100 , 300) , "http://www.chendd.cn" , -0.5 , 0.5f);

        //给较长图添加一屏一个水印

        File longImg = new File(srcFile.getParent() , "加长版.jpg");

        watermarkLongImage(longImg, new File(longImg.getParent() , "加长版图片水印.jpg"));

        System.out.println("=========图片格式转换貌似使用较少，此处省略=========");

    }

    private static void watermarkLongImage(File srcFile , File outFile) {

        //从原图中找出300x300的大小来显示水印文本

        BufferedImage bi = new BufferedImage(300, 100 ,BufferedImage.TYPE_INT_RGB);

        Graphics2D g = bi.createGraphics();

        //设置绘图区域透明

        bi = g.getDeviceConfiguration().createCompatibleImage(300, 100,Transparency.TRANSLUCENT);

        g.dispose();

        //字体、字体大小，透明度，旋转角度

        g = bi.createGraphics();

        g.setFont(new Font("微软雅黑", Font.BOLD, 26));

        char[] data = "http://www.chendd.cn".toCharArray();

        g.setColor(Color.RED);

        //设置文本显示坐标，以上述中的300x300位置为0,0点

        g.drawChars(data, 0, data.length, 10, 20);

        try {

            BufferedImage image = ImageIO.read(srcFile);

            int height = image.getHeight();

            //假设此图在手机端访问，手机的高度为400，则每超过400部分显示一个水印

            Thumbnails.Builder<File> builder = Thumbnails.of(srcFile).scale(1);

            int mod = (int) Math.ceil(height / 400);

            for(int i=1 ; i < mod ; i++){

                int x = 200;

                int y = (i) * 400;

                System.out.println(x + "," + y);

                builder.watermark(new Coordinate(x , y), bi, 1);

            }

            builder.toFile(outFile);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

}
