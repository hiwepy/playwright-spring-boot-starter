package com.microsoft.playwright.spring.boot.playwright.util;

import com.microsoft.playwright.spring.boot.playwright.checker.FastImageComparator;
import com.microsoft.playwright.spring.boot.playwright.checker.ImagePixelCache;
import com.microsoft.playwright.spring.boot.playwright.enums.PDPageSize;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Coordinate;
import net.coobird.thumbnailator.geometry.Position;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.resizers.configurations.AlphaInterpolation;
import net.coobird.thumbnailator.resizers.configurations.Antialiasing;
import net.coobird.thumbnailator.resizers.configurations.Dithering;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Slf4j
public class ImageUtil {

    public static String IMAGE_TYPE = "png";
    public static String IMAGE_WATERMARK_TEXT = "空白页面";
    public static BufferedImage WHITE_A3 = ImageUtil.getWhiteImageWithWatermark(Float.valueOf(PDPageSize.A3.getRectangle().getWidth()).intValue(),
            Float.valueOf(PDPageSize.A3.getRectangle().getHeight()).intValue(), IMAGE_WATERMARK_TEXT);
    public static BufferedImage WHITE_A4 = ImageUtil.getWhiteImageWithWatermark(Float.valueOf(PDPageSize.A4.getRectangle().getWidth()).intValue(),
            Float.valueOf(PDPageSize.A4.getRectangle().getHeight()).intValue(), IMAGE_WATERMARK_TEXT);
    public static long WHITE_A3_SIZE = ImageUtil.getImageFileSize(WHITE_A3, IMAGE_TYPE);
    public static long WHITE_A4_SIZE = ImageUtil.getImageFileSize(WHITE_A4, IMAGE_TYPE);
    /**
     * 获取图片文件大小
     * @param image 图片
     * @param formatName 图片格式
     * @return 图片文件大小
     */
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

    /**
     * 获取指定宽度、高度和颜色的图片
     * @param width 宽度
     * @param height 高度
     * @param color 颜色
     * @return 指定宽度、高度和颜色的图片
     */
    public static BufferedImage getImageOnlyColor(int width, int height, Color color) {
        // Create a buffered image with the specified width and height
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Get the graphics object from the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();
        // Set the background color to white
        g2d.setColor(color);
        // Fill the entire image with the white background
        g2d.fillRect(0, 0, width, height);
        // Dispose the graphics object
        g2d.dispose();
        return bufferedImage;
    }

    /**
     * 获取指定宽度、高度和颜色的图片
     * @param width 宽度
     * @param height 高度
     * @param color 颜色
     * @return 指定宽度、高度和颜色的图片
     */
    public static BufferedImage getImageOnlyColor(int width, int height, Color color, String text) {
        // Create a buffered image with the specified width and height
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Get the graphics object from the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();
        // Set the background color to white
        g2d.setColor(color);
        // Fill the entire image with the white background
        g2d.fillRect(0, 0, width, height);
        // 添加水印
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString(text, width / 2 - text.length() * 10, height / 2 + 10);
        // Dispose the graphics object
        g2d.dispose();
        return bufferedImage;
    }

    /**
     * 获取白色图片
     * @param width 宽度
     * @param height 高度
     * @return 白色图片
     */
    public static BufferedImage getWhiteImage(int width, int height) {
        return getImageOnlyColor(width, height, Color.WHITE);
    }

    /**
     * 获取白色图片
     * @param width 宽度
     * @param height 高度
     * @param text 文本
     * @return 白色图片
     */
    public static BufferedImage getWhiteImageWithWatermark(int width, int height, String text) {
        return watermark(getImageOnlyColor(width, height, Color.WHITE),  Positions.CENTER , text , -0.5 , 0.8f);
    }

    /**
     * 判断图片是否为白色
     * @param image 图片
     * @return 是否为白色
     */
    public static boolean isWhiteImage(BufferedImage image) {
        return isImageOnlyColor(image, Color.WHITE);
    }

    /**
     * 判断图片是否为指定颜色
     * @param image 图片
     * @param color 颜色
     * @return 是否为指定颜色
     */
    public static boolean isImageOnlyColor(BufferedImage image, Color color) {
        int width = image.getWidth();
        int height = image.getHeight();
        int targetRgb = color.getRGB() & 0x00FFFFFF;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // 只判断RGB
                int rgb = image.getRGB(x, y) & 0x00FFFFFF;
                if (rgb != targetRgb) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 判断图片是否为白色
     * @param image 图片        
     * @param percent 占比
     * @return 白色占比是否大于等于指定占比
     */
    public static boolean isWhiteImageOutPercent(BufferedImage image, float percent) {
        return isImageColorOutPercent(image, Color.WHITE, percent);
    }

    /**
     * 判断图片包含指定颜色占比
     * @param image 图片
     * @param color 颜色
     * @param percent 占比
     * @return 指定颜色占比是否大于等于指定占比
     */
    public static boolean isImageColorOutPercent(BufferedImage image, Color color, float percent) {
        int width = image.getWidth();
        int height = image.getHeight();
        float count = 0;
        int targetRgb = color.getRGB() & 0x00FFFFFF;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y) & 0x00FFFFFF;
                if (rgb == targetRgb) {
                    count ++;
                }
            }
        }
        // 计算指定颜色占比
        return (count / (width * height)) >= percent / 100f;
    }

    /**
     * 将图片缩放到A3纸张大小
     * @param image 图片
     * @return 缩放到A3纸张大小的图片
     * @throws IOException 缩放异常
     */
    public static BufferedImage scaleToA3(BufferedImage image) throws IOException {
        return scaleToA3(image, 100);
    }

    /**
     * 将图片缩放到A3纸张大小
     * @param image 图片
     * @param quality 质量 1-100
     * @return 缩放到A3纸张大小的图片
     * @throws IOException 缩放异常
     */
    public static BufferedImage scaleToA3(BufferedImage image, Integer quality) throws IOException {
        return scaleTo(image, PDPageSize.A3, quality);
    }

    /**
     * 将图片缩放到A4纸张大小
     * @param image 图片
     * @return 缩放到A4纸张大小的图片
     * @throws IOException 缩放异常
     */
    public static BufferedImage scaleToA4(BufferedImage image) throws IOException {
        return scaleToA4(image, 100);
    }

    /**
     * 将图片缩放到A4纸张大小
     * @param image 图片
     * @param quality 质量 1-100
     * @return 缩放到A4纸张大小的图片
     * @throws IOException 缩放异常
     */
    public static BufferedImage scaleToA4(BufferedImage image, Integer quality) throws IOException {
        return scaleTo(image, PDPageSize.A4, quality);
    }

    /**
     * 将图片缩放到指定纸张大小
     * @param image 图片
     * @param pdPageSize 纸张大小
     * @return 缩放到指定纸张大小的图片
     * @throws IOException 缩放异常
     */
    public static BufferedImage scaleTo(BufferedImage image, PDPageSize pdPageSize, Integer quality) throws IOException {
        PDRectangle pdRectangle = pdPageSize.getRectangle();
        return scaleTo(image, pdRectangle.getWidth(), pdRectangle.getHeight(), quality);
    }

    /**
     * 将图片缩放到指定纸张大小
     * @param image 图片
     * @param width 宽度
     * @param height 高度
     * @return 缩放到指定高宽的图片
     * @throws IOException 缩放异常
     */
    public static BufferedImage scaleTo(BufferedImage image, float width, float height, Integer quality) throws IOException {
        if(isNeedScale(image, width, height)){
            // 计算缩放比例，使图像适应页面的大小
            float scaleFactor = Math.min(width / image.getWidth(), width / image.getHeight());
            // 计算缩放后的图像尺寸
            float scaledWidth = width / image.getWidth();
            float scaledHeight = height / image.getHeight();
            log.info("scaleFactor:{}, scaledWidth:{} , scaledHeight:{}  ", scaleFactor, scaledWidth, scaledHeight);
            return Thumbnails.of(image)
                    .alphaInterpolation(AlphaInterpolation.QUALITY)
                    .antialiasing(Antialiasing.OFF)
                    .dithering(Dithering.ENABLE)
                    .scale(scaledWidth, scaledHeight)
                    .outputQuality(quality / 100f)
                    .asBufferedImage();
        }
        return image;
    }

    /**
     * 是否需要进行缩放
     * @param image 图片
     * @param pdPageSize 纸张大小
     * @return 是否需要进行缩放
     */
    public static boolean isNeedScale(BufferedImage image, PDPageSize pdPageSize) {
        PDRectangle pdRectangle = pdPageSize.getRectangle();
        return isNeedScale(image, pdRectangle.getWidth(), pdRectangle.getHeight());
    }

    /**
     * 是否需要进行缩放
     * @param image 图片
     * @param width 宽度
     * @param height 高度
     * @return 是否需要进行缩放
     */
    public static boolean isNeedScale(BufferedImage image, float width, float height) {
        return image.getWidth() != width || image.getHeight() != height;
    }

    /**
     * 获取图片像素
     * @param image 图片
     * @return 图片像素
     */
    public static int[][] getImagePixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] pixels = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixels[x][y] = image.getRGB(x, y);
            }
        }
        return pixels;
    }

    /**
     * 计算两张图片的相似度
     * @param pixels1 图片1的像素
     * @param pixels2 图片2的像素
     * @return 相似度
     */
    public static double calculateSimilarity(int[][] pixels1, int[][] pixels2) {

        if (pixels1.length == 0 || pixels2.length == 0 || pixels1.length != pixels2.length || pixels1[0].length != pixels2[0].length) {
            log.warn("Image dimensions do not match for similarity calculation.");
            // 或者抛出异常，或者返回一个表示无效比较的值
            return 0.0;
        }

        int width = pixels1.length;
        int height = pixels1[0].length;
        int count = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // 如果 pixels2 尺寸小于 pixels1，这里会 ArrayIndexOutOfBoundsException
                if (pixels1[x][y] == pixels2[x][y]) {
                    count++;
                }
            }
        }

        return (double) count / (width * height);
    }

    /**
     * 按宽高进行缩放，此时有几个图片宽度与高度大小的逻辑处理
     * @param srcFile 源文件
     * @param outFile 输出文件
     * @param width 宽度
     * @param height 高度
     */
    public static void zoomSize(File srcFile , File outFile , int width , int height){
        try {
            Thumbnails.of(srcFile).size(width, height).toFile(outFile);
        } catch (IOException e) {
            log.error("zoomSize error", e);
        }
    }

    /**

     * 将图片进行等比例缩放，0~~1之间的为缩小，大于1的为放大
     * @param srcFile 源文件
     * @param outFile 输出文件
     * @param scale 缩放比例
     */
    public static void zoomScaling(File srcFile , File outFile , double scale){
        try {
            Thumbnails.of(srcFile).scale(scale).toFile(outFile);
        } catch (IOException e) {
            log.error("zoomScaling error", e);
        }
    }

    /**
     * 按角度旋转
     * @param srcFile 源文件
     * @param outFile 输出文件
     * @param rotate 旋转角度
     */
    public static void rotateAngle(File srcFile , File outFile , double rotate){
        try {
            Thumbnails.of(srcFile).scale(1).rotate(rotate).toFile(outFile);
        } catch (IOException e) {
            log.error("rotateAngle error", e);
        }
    }

    /**
     * 按坐标位置裁切图片，先按位置获取指定的图片范围，再按指定宽高存储
     * @param srcFile 源文件
     * @param outFile 输出文件
     * @param postition 位置
     * @param width 宽度
     * @param height 高度
     * @param x 坐标x
     * @param y 坐标y
     */
    public static void corpRegion(File srcFile , File outFile , Positions postition , int width , int height , int x , int y){
        try {
            Thumbnails.of(srcFile).sourceRegion(postition, x, y).size(width, height).toFile(outFile);
        } catch (IOException e) {
            log.error("corpRegion error", e);
        }
    }

    /**
     * 按坐标位置裁切图片，先按位置获取指定的图片范围，再按指定宽高存储
     * x1，x2为左上角的坐标点
     * y1，y2为以左上角为0,0坐标位置的相对坐标（或者x1+要裁切图片宽度=y1,y1+要裁切图片高度=y2的坐标）
     * @param srcFile 源文件
     * @param outFile 输出文件
     * @param width 宽度
     * @param height 高度
     * @param x1 坐标x1
     * @param y1 坐标y1
     * @param x2 坐标x2
     */
    public static void corpRegion(File srcFile , File outFile , int width , int height , int x1 , int y1 , int x2 , int y2){
        try {
            Thumbnails.of(srcFile).sourceRegion(x1, y1, x2, y2).size(width, height).keepAspectRatio(false).toFile(outFile);
        } catch (IOException e) {
            log.error("corpRegion error", e);
        }
    }

    /**
     * 生成文本水印图片
     * @param text 文本
     * @param rotate 旋转角度
     */
    public static BufferedImage watermarkImage(String text , double rotate){
        // 从原图中找出300x300的大小来显示水印文本
        BufferedImage bi = new BufferedImage(300, 300 ,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        // 设置绘图区域透明
        bi = g.getDeviceConfiguration().createCompatibleImage(300, 300,Transparency.TRANSLUCENT);
        g.dispose();
        // 字体、字体大小，透明度，旋转角度
        g = bi.createGraphics();
        g.setFont(new Font("微软雅黑", Font.BOLD, 26));
        char[] data = text.toCharArray();
        g.rotate(rotate);
        g.setColor(Color.RED);
        // 设置文本显示坐标，以上述中的300x300位置为0,0点
        g.drawChars(data, 0, data.length, -70, 200);
        return bi;
    }

    /**
     * 添加文本水印，也可添加图片水印
     * @param srcImage 源文件
     * @param position 位置
     * @param text 文本
     * @param rotate 旋转角度
     * @param opacity 透明度
     */
    public static BufferedImage watermark(BufferedImage srcImage , Position position , String text , double rotate , float opacity){
        try {
            BufferedImage watermarkImage = watermarkImage(text, rotate);
           return Thumbnails.of(srcImage).scale(1).watermark(position, watermarkImage, opacity).asBufferedImage();
        } catch (IOException e) {
            log.error("watermark error", e);
            return srcImage;
        }
    }

    /**
     * 添加文本水印，也可添加图片水印
     * @param srcFile 源文件
     * @param outFile 输出文件
     * @param position 位置
     * @param text 文本
     * @param rotate 旋转角度
     * @param opacity 透明度
     */
    public static void watermark(File srcFile , File outFile , Position position , String text , double rotate , float opacity){
        try {
            BufferedImage watermarkImage = watermarkImage(text, rotate);
            Thumbnails.of(srcFile).scale(1).watermark(position, watermarkImage, opacity).toFile(outFile);
        } catch (IOException e) {
            log.error("watermark error", e);
        }
    }

    public static void main(String[] args) throws IOException {

        //ImageIO.write(WHITE_A3, IMAGE_TYPE, new File("/Users/wandl/tmp/1924671360978698240/white-a3.png"));

        BufferedImage wImage =  ImageIO.read(new File("/Users/wandl/tmp/1924671360978698240/white-a3.png"));
        if (isWhiteImageOutPercent(wImage, 85f)){
            System.out.println("白色图片");
        } else {
            System.out.println("非白色图片");
        }

        PDPageSize pdfPageSize = PDPageSize.A4;
        BufferedImage background =  ImageIO.read(new File("/Users/wandl/tmp/1924671360978698240/7.png"));
        BufferedImage pdfImage =  ImageIO.read(new File("/Users/wandl/tmp/1924671360978698240/10.png"));
        background = ImageUtil.scaleTo(background, pdfPageSize, 100);
        pdfImage = ImageUtil.scaleTo(pdfImage, pdfPageSize, 100);
        double similarity = FastImageComparator.compare(new ImagePixelCache(background), new ImagePixelCache(pdfImage));
        if (similarity > 80){
            System.out.println("截图图片检查未通过，背景图片相似度过高。");
        } else {
            System.out.println("截图图片检查通过，背景图片相似度过低。");
        }


        /*BufferedImage image =  ImageIO.read(new File("/Users/wandl/tmp/1924671360978698240/7.png"));
        if (isWhiteImageOutPercent(image, 90f)){
            System.out.println("白色图片");
        } else {
            System.out.println("非白色图片");
        }*/
/*

        ImageIO.write(WHITE_A3, IMAGE_TYPE, new File("white-a3.png"));

        ImageUtil.watermark(new File("D:/growth/output/707773105468432384/1.png"), new File("D:/growth/output/707773105468432384/1-2.png"), Positions.CENTER, "http://www.chendd.cn", 0, 0.5f);

        File srcFile = new File("D:/growth/output/707773105468432384/1.png");

        BufferedImage image2 = scaleToA3(ImageIO.read(srcFile));
        ImageIO.write(image2, "png", new File("D:/growth/output/707773105468432384/1-1.png"));

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

        System.out.println("=========图片格式转换貌似使用较少，此处省略=========");*/

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
