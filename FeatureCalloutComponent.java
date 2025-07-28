//Added By Aadesh For Feature Callout Upload on 18-07-2023 
package com.contexio.catalog.serviceimpl;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.contexio.catalog.constants.Constants;
import com.contexio.catalog.dto.FeatureCalloutDataDTO;
import com.contexio.catalog.dto.FeatureCalloutmageProperties;
import com.contexio.catalog.repository.FeatureCalloutRepository;
import com.contexio.catalog.service.FeatureCalloutComponentService;
import com.mongodb.client.result.UpdateResult;


/**
 * Code for A+ Images Feature Callout
 * 
 * @author Jaydeep.Patil
 * @since 28-05-2023
 * @version 1.0
 */
@Service
public class FeatureCalloutComponent implements FeatureCalloutComponentService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCalloutComponent.class);
	private static int imgWidth;
	private static int imgHeight;

	private static int margin_topBottom;
	private static int margin_leftRight;

	private static int centerImageCanvasWidth;
	private static int centerImageCanvasHeight;

	private static int featureImageWidth;
	private static int featureImageHeight;
	private static int featureTextdist;
	private static int featureTextMaxHeight;

	private static int textImgDistance;
	static double tolerance = 0.1;

	private static final String temp_folder = "D:\\WorkSpace\\Technical Team\\Files\\Feature Callout\\temp\\";
	private static final String cropPath = "D:\\WorkSpace\\Technical Team\\Files\\Feature Callout\\Imaging\\Crop\\";
	private static final String verticalPath = "D:\\WorkSpace\\Technical Team\\Files\\Feature Callout\\Imaging\\Vertical\\";
	private static final String horizontalPath = "D:\\WorkSpace\\Technical Team\\Files\\Feature Callout\\Imaging\\Horizontal\\";
	private static final String canvasPath = "D:\\WorkSpace\\Technical Team\\Files\\Feature Callout\\Imaging\\Canvas\\";
	private static final String resizedCrop = "D:\\WorkSpace\\Technical Team\\Files\\Feature Callout\\Imaging\\ResizedCrop\\";

	@Autowired
	FeatureCalloutRepository featurecalloutrepository;
	Constants modelConstants = new Constants();
	private static Hashtable<String, Integer> headerRow = new Hashtable<String, Integer>();
	
	@Autowired
	private AmazonS3 amazonS3;
	@Value("${aws.s3.bucket}")
	private String bucketName;
	@Autowired
	MongoTemplate mongoTemplate;
	
	//Added By Aadesh for Patch Creation
	@Override
	public String generateFeatureImage(String clientId,String projectId,String batchName,String featureCreationType) {	
		String output_file="";
		
		List<FeatureCalloutDataDTO> batchDetails = featurecalloutrepository.fetchfeatureCalloutBatchData(clientId, projectId,batchName);
	
		for(int a=0;a<batchDetails.size();a++) 
		{		
			
			//String input_file = "D:\\WorkSpace\\Technical Team\\Files\\Feature Callout\\Woltop_23rd May\\Input\\Flipkart_Feature_callout_template_v2_23rd May 2023_AaDu.xlsx";		
			File file = null;
			file = new File(temp_folder);
			if (!file.exists())
				file.mkdirs();
	
			File filecheck=null;
			if(featureCreationType.equals("RPD Creation")) 
			{
				
				String filePath = modelConstants.getFeatureCalloutBatchImages() + batchName + "/ProductImages/" + batchDetails.get(a).getProperties().get("SKU_ID");
				filecheck = new File(filePath);
			}
			else if(featureCreationType.equals("Patch Creation")) 
			{
				
				String filePath = modelConstants.getFeatureCalloutBatchImages() + batchName + "/BackgroundImages/" + batchDetails.get(a).getProperties().get("SKU_ID");
				 filecheck = new File(filePath);				
			} 
			else if(featureCreationType.equals("Infographics")) 
			{
				
				String filePath = modelConstants.getFeatureCalloutBatchImages() + batchName + "/BackgroundImages/" + batchDetails.get(a).getProperties().get("SKU_ID");
				filecheck = new File(filePath);				
			} 

			if (filecheck.exists()) {
			try {
						updateStatus_featuredBatch( clientId,  projectId, batchDetails.get(a).getBatchName());
						FeatureCalloutmageProperties obj = new FeatureCalloutmageProperties();
						Cell cell = null;
						//cell = row.getCell(headerRow.get("input_folder"));
						obj.setInput_folder(batchDetails.get(a).getProperties().get("input_folder"));
						//cell = row.getCell(headerRow.get("output_folder"));
						obj.setOutput_folder(batchDetails.get(a).getProperties().get("output_folder"));
						//cell = row.getCell(headerRow.get("SKU ID"));
						obj.setSKU_ID(batchDetails.get(a).getProperties().get("SKU_ID"));
						//cell = row.getCell(headerRow.get("Image Width"));
						obj.setImage_Width(Integer.parseInt(batchDetails.get(a).getProperties().get("Image_Width")));
						//cell = row.getCell(headerRow.get("Image Height"));
						obj.setImage_Height(Integer.parseInt(batchDetails.get(a).getProperties().get("Image_Height")));
						//cell = row.getCell(headerRow.get("Top_Bottom Margin"));
						obj.setTopBottom_Margin(Integer.parseInt(batchDetails.get(a).getProperties().get("Top_Bottom_Margin")));
						//cell = row.getCell(headerRow.get("Left_Right Margin"));
						obj.setLeftRight_Margin(Integer.parseInt(batchDetails.get(a).getProperties().get("Left_Right_Margin")));
						//cell = row.getCell(headerRow.get("centerImageCanvasWidth"));
						obj.setCenterImageCanvasWidth(Integer.parseInt(batchDetails.get(a).getProperties().get("centerImageCanvasWidth")));
						//cell = row.getCell(headerRow.get("centerImageCanvasHeight"));
						obj.setCenterImageCanvasHeight(Integer.parseInt(batchDetails.get(a).getProperties().get("centerImageCanvasHeight")));
						//cell = row.getCell(headerRow.get("centerImageHorizontalWidth"));
						obj.setCenterImageCanvasWidthHorizontal(Integer.parseInt(batchDetails.get(a).getProperties().get("centerImageHorizontalWidth")));
						//cell = row.getCell(headerRow.get("centerImageHorizontalHeight"));
						obj.setCenterImageCanvasHeightHorizontal(Integer.parseInt(batchDetails.get(a).getProperties().get("centerImageHorizontalHeight")));
						//cell = row.getCell(headerRow.get("Text Img Distance"));
						obj.setText_Img_Distance(Integer.parseInt(batchDetails.get(a).getProperties().get("Text_Img_Distance")));
						//cell = row.getCell(headerRow.get("Background Color"));
						obj.setBackground_Color(batchDetails.get(a).getProperties().get("Background_Color"));
						//cell = row.getCell(headerRow.get("Layout"));
						obj.setLayout(batchDetails.get(a).getProperties().get("Layout"));						
						//cell = row.getCell(headerRow.get("Feature_Count"));
						obj.setFeature_Count(Integer.parseInt(batchDetails.get(a).getProperties().get("Feature_Count")));					
						//cell = row.getCell(headerRow.get("Background Image Path"));
						obj.setBackgroundImagePath(batchDetails.get(a).getProperties().get("Background_Image_Path"));
						obj.setBackgroundImageName(batchDetails.get(a).getProperties().get("Background_Image_Name"));
						//cell = row.getCell(headerRow.get("Logo Path"));
						obj.setLogoImagePath(batchDetails.get(a).getProperties().get("Logo_Path"));
						//cell = row.getCell(headerRow.get("Logo Name"));
						obj.setLogoImageName(batchDetails.get(a).getProperties().get("Logo_Name"));
						//cell = row.getCell(headerRow.get("Header 1"));
						obj.setHeader1(batchDetails.get(a).getProperties().get("Header_1"));
						obj.setHeader2(batchDetails.get(a).getProperties().get("Header_2"));
						obj.setHeader3(batchDetails.get(a).getProperties().get("Header_3"));
						obj.setHeader4(batchDetails.get(a).getProperties().get("Header_4"));
						obj.setHeader5(batchDetails.get(a).getProperties().get("Header_5"));
						obj.setHeader6(batchDetails.get(a).getProperties().get("Header_6"));
						//cell = row.getCell(headerRow.get("Value 1"));
						obj.setKeyVal1(batchDetails.get(a).getProperties().get("Value_1"));
						//cell = row.getCell(headerRow.get("Value 2"));
						obj.setKeyVal2(batchDetails.get(a).getProperties().get("Value_2"));
						//cell = row.getCell(headerRow.get("Value 3"));
						obj.setKeyVal3(batchDetails.get(a).getProperties().get("Value_3"));
						//cell = row.getCell(headerRow.get("Value 4"));
						obj.setKeyVal4(batchDetails.get(a).getProperties().get("Value_4"));	
						obj.setKeyVal5(batchDetails.get(a).getProperties().get("Value_5"));	
						obj.setKeyVal6(batchDetails.get(a).getProperties().get("Value_6"));	
						//cell = row.getCell(headerRow.get("Font Color"));
						obj.setFontColor(batchDetails.get(a).getProperties().get("Font_Color"));						
						//cell = row.getCell(headerRow.get("Font Size"));
						obj.setFontSize(Integer.parseInt(batchDetails.get(a).getProperties().get("Font_Size")));									
						obj.setOutputImageName(batchDetails.get(a).getProperties().get("Output_Image_Name"));
						if(featureCreationType.equals("RPD Creation")) 
						{					
							output_file=rpdCreation(obj,batchName);							
						}
						else if(featureCreationType.equals("Patch Creation")) 
						{						
							output_file=createFeatureCallImage1(obj,batchName);
						} 
						else if(featureCreationType.equals("Infographics")) 
						{							
							output_file=tataInfographics(obj,batchName);
						} 
							
						/*output_file=createFeatureCallImage1(obj,batchName);*/
						
						String outputfilePath = modelConstants.getFeatureCalloutOutputImage()+batchName+"/"+batchDetails.get(a).getProperties().get("Output_Image_Name");
						File fileoutputcheck = new File(outputfilePath);
						//LOGGER.info("----"+modelConstants.getFeatureCalloutOutputImage()+batchName+"/"+batchDetails.get(a).getProperties().get("SKU_ID"));
						if (fileoutputcheck.exists()) 
						{					
							updateStatus_featuredImage(clientId, projectId,batchName,batchDetails.get(a).getProperties().get("Output_Image_Name"));
						}
			}
			catch (Exception e) {
				
				LOGGER.info("Error in Input provided. Please check the input entered and try again.");
				LOGGER.info("Process Aborted."+e);
			}
			}
			else {
				output_file="Error. Please check the Batch Input and Images are Uploaded and try again.";
			}
		}
		// delete from temp directory
		File[] temp = new File(temp_folder).listFiles();
		if(temp.length!=0) 
		{
			for (File tempfiles : temp) {
				tempfiles.delete();
			}
		}
	

		// delete from crop directory
		File[] tempCrop = new File(cropPath).listFiles();
		if(tempCrop.length!=0) 
		{
			for (File tempfiles : tempCrop) {
			tempfiles.delete();
			}

		}
		// delete from horizontalPath directory
		File horizontalDir = new File(horizontalPath);
		if (horizontalDir.exists() && horizontalDir.isDirectory()) {
		File[] tempResizedHorizontal = new File(horizontalPath).listFiles();
		if(tempResizedHorizontal.length!=0) 
		{
			for (File tempfiles : tempResizedHorizontal) {
				tempfiles.delete();
			}
		}
		}
		
		
		// delete from verticalPath directory
		File verticalPathDir = new File(verticalPath);
		if (verticalPathDir.exists() && verticalPathDir.isDirectory()) {
		File[] tempResizedVertical = new File(verticalPath).listFiles();
		if(tempResizedVertical.length!=0) 
		{
			for (File tempfiles : tempResizedVertical) {
				tempfiles.delete();
			}
		}
		}
		// delete from verticalPath directory
		File resizedCropDir = new File(resizedCrop);
		if (resizedCropDir.exists() && resizedCropDir.isDirectory()) {
		File[] tempResizedCrop = new File(resizedCrop).listFiles();
		if(tempResizedCrop.length!=0) 
		{
			for (File tempfiles : tempResizedCrop) {
				tempfiles.delete();
			}
		}
		}
		// delete from canvasPath directory
		File canvasPathDir = new File(canvasPath);
		if (canvasPathDir.exists() && canvasPathDir.isDirectory()) {
		File[] tempCanvas = new File(canvasPath).listFiles();
		if(tempCanvas.length!=0) 
		{
			for (File tempfiles : tempCanvas) {
				tempfiles.delete();
			}
		}
		}
	
		
			return output_file;
		}
		
	//}
	
	@SuppressWarnings("unused")
	private static BufferedImage resizeImage(BufferedImage originalImage, int type) {
		FeatureCalloutmageProperties obj = new FeatureCalloutmageProperties();
		BufferedImage resizedImage = new BufferedImage(obj.getImage_Width(), obj.getImage_Height(),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, obj.getImage_Width(), obj.getImage_Height(), null);
		g.dispose();

		return resizedImage;
	}
	
	@SuppressWarnings("unused")
	private static BufferedImage resizeCrop(BufferedImage img, int height, int width) {
		Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = resized.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return resized;
	}

	public static int centerCalculationX(int centerImageWidth, int centerImageCanvasWidth, int imgWidth) {
		int centerCanvasX = (imgWidth / 2) - (centerImageCanvasWidth / 2);
		int centerX = centerCanvasX + ((centerImageCanvasWidth - centerImageWidth) / 2);

		return centerX;
	}

	public static int centerCalculationY(int margin_topBottom, int centerImageHeight, int centerImageCanvasHeight) {
		int centercanvasY = (imgHeight / 2) - (centerImageCanvasHeight / 2);
		int centerY;
		if (centercanvasY < margin_topBottom) {
			centerY = margin_topBottom;
		} else {
			centerY = centercanvasY + (centerImageCanvasHeight - centerImageHeight) / 2;
		}
		return centerY;
	}

	public static void drawStringMultiLine(Graphics g, String text, int lineWidth, int x, int y, Rectangle rect,
			Font font) {
		
		FontMetrics m = g.getFontMetrics();
		if (m.stringWidth(text) < lineWidth) {
			drawCenteredString(g, text, rect, font);
		} else {

			String[] words = text.split(" ");
			String currentLine = "";
			for (int i = 0; i < words.length; i++) {
				/*
				 * System.out.println(" words[i] = "+ words[i]);
				 * System.out.println(" words[1] = "+ words[1]);
				 */
				// String wordF = words[1];
				if (m.stringWidth(currentLine) < lineWidth) {
					String temp = currentLine + " " + words[i];
					if (m.stringWidth(temp) < lineWidth) {
						currentLine += " " + words[i];
					} else {
						// System.out.println("else main currentLine: "+currentLine);
						drawCenteredString(g, currentLine, rect, font);
						rect.y = rect.y + 70;
						y += m.getHeight();

						currentLine = words[i];
					}
				}
			}
			// rect.y = rect.y + 15;
			y += m.getHeight();
			drawCenteredString(g, currentLine, rect, font);
		}
	}

	//Added By Aadesh for Patch Creation Script
	public String createFeatureCallImage1(FeatureCalloutmageProperties obj,String batchName) throws Exception {
	String message="";
		try {
		
			//File imgfile = new File(obj.getInput_folder() + obj.getSKU_ID());
			File imgfile = new File(modelConstants.getFeatureCalloutBatchImages()+batchName+"/"+obj.getSKU_ID());
			if (imgfile.getName().equals("Thumbs.db")) {
				imgfile.delete();
			} else {
				
				imgWidth = obj.getImage_Width();
				imgHeight = obj.getImage_Height();
				margin_topBottom = obj.getTopBottom_Margin();
				margin_leftRight = obj.getLeftRight_Margin();
				centerImageCanvasWidth = obj.getCenterImageCanvasWidth();
				centerImageCanvasHeight = obj.getCenterImageCanvasHeight();
				featureImageWidth = obj.getFeature_Image_Width();
				featureImageHeight = obj.getFeature_Image_Height();
				featureTextdist = obj.getFeature_Text_distance();
				featureTextMaxHeight = obj.getFeature_Text_Max_Height();
				textImgDistance = obj.getText_Img_Distance();
				// ___________________________-----_______________________________//
				
				//LOGGER.info("-----"+obj.getBackgroundImagePath());
				//LOGGER.info("-----"+modelConstants.getFeatureCalloutBatchImages()+batchName+"/"+"ProductImages/"+obj.getSKU_ID());
				String filePath = modelConstants.getFeatureCalloutBatchImages() + batchName + "/ProductImages/" + obj.getSKU_ID();
				File file = new File(filePath);

				if (file.exists()) {
				//BufferedImage bufferedImage = ImageIO.read(new File(obj.getBackgroundImagePath()));
				BufferedImage bufferedImage = ImageIO.read(new File(modelConstants.getFeatureCalloutBatchImages()+batchName+"/"+"ProductImages/"+obj.getSKU_ID()));
				
				//BufferedImage bufferedImage = Scalr.resize(bufferedImageBG, Method.ULTRA_QUALITY, obj.getImage_Width(), obj.getImage_Height());
				
				File whiteImageFile = new File(canvasPath);
				if (!whiteImageFile.exists())
					whiteImageFile.mkdirs();
				if (bufferedImage != null) {
					ImageIO.write(bufferedImage, "jpg", new File(whiteImageFile, obj.getSKU_ID()));
					// create the new image, canvas size is the max. of both image sizes
					int w = Math.max(bufferedImage.getWidth(), bufferedImage.getWidth());
					int h = Math.max(bufferedImage.getHeight(), bufferedImage.getHeight());
					BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
					
					Graphics2D g = (Graphics2D) combined.getGraphics();
					
					String header1 = "";
					String value1 = "";
					String value2 = "";
					String value3 = "";
					String value4 = "";
					
					BufferedImage transImage1 = null;
					BufferedImage transImage2 = null;
					
					try {
						transImage1 = ImageIO.read(new File(modelConstants.getFeatireCalloutSupportImage()+"1333x120.png"));
						
					}catch (Exception e) {
						
						e.getMessage();
					}
					
					try {
						
						transImage2 = ImageIO.read(new File(modelConstants.getFeatireCalloutSupportImage()+"Transparent patch 2.png"));
					}catch (Exception e) {
						
						e.getMessage();
					}
					
					try {
						
						header1 = obj.getHeader1();
					} catch (Exception e) {
						
						e.getMessage();
					}
					
					try {
						value1 = obj.getKeyVal1();
					
					} catch (Exception e) {
						
						e.getMessage();
					}
					
					try {
						
						value2 = obj.getKeyVal2();
					} catch (Exception e) {
					
						e.getMessage();
					}
					
					try {
						
						value3 = obj.getKeyVal3();
					} catch (Exception e) {
					
						e.getMessage();
					}
					
					try {
					
						value4 = obj.getKeyVal4();
					} catch (Exception e) {
						
						e.getMessage();
					}
					
					// Harcoded textbox size
					
					
					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
				
					if (obj.getLayout().equalsIgnoreCase(("Lifestyle Image"))) {
					 	
					 	Rectangle rect = new Rectangle();
						rect.width = 2000;
						rect.height = 3310;
						
						g.drawImage(bufferedImage, 0, 0, null); // bg Canvas
						
						BufferedImage transparentPath = Scalr.resize(transImage1, Method.ULTRA_QUALITY, 1800,120);
						g.drawImage(transparentPath, 100, 1580, null); // bg Canvas
						
						g.setColor(Color.BLACK);
						g.setFont(new Font(obj.getFontName(), Font.BOLD, obj.getFontSize()));
						
						//g.drawString(header1, 430, 1670);
						drawCenteredString(g, header1, rect, new Font(obj.getFontName(), Font.BOLD, obj.getFontSize()));
						
					} else if (obj.getLayout().equalsIgnoreCase(("Features2"))) {
						
						g.drawImage(bufferedImage, 0, 0, null); // bg Canvas
						
						BufferedImage transparentPath = Scalr.resize(transImage2, Method.ULTRA_QUALITY, 1600,110);
						g.drawImage(transparentPath, 450, 1, null); // bg Canvas
						
						g.setColor(Color.BLACK);
						g.setFont(new Font(obj.getFontName(), Font.BOLD, 65));
						
						g.drawString("Features", 1160, 200);
						
						g.setColor(Color.BLACK);
						g.setFont(new Font(obj.getFontName(), Font.PLAIN, obj.getFontSize()));
						//g.drawString(value1, 700, 350);
						
						int headerX = 1120;
						int headerY = 300;
						
						int strLength = 31;
						
						String h1 = WordUtils.wrap(value1, strLength);
						String[] F1 =  h1.split(System.lineSeparator());
						String abc="";
						int dim = 45;
						int counter= 1;
						for(int a=0; a<F1.length; a++) {
							abc += F1[a] + "\n";
							
							if(a==0) {
								g.drawString("• "+F1[0], headerX, headerY+(dim*counter++));
							}else {
								g.drawString("   "+F1[a], headerX, headerY+(dim*counter++));
							}
						}
						
						String h2 = WordUtils.wrap(value2, strLength);
						String[] F2 =  h2.split(System.lineSeparator());
						String abc2="";
						for(int a=0; a<F2.length; a++) {
							abc2 += F2[a] + "\n";
							
							if(a==0) {
								g.drawString("• "+F2[0], headerX, (headerY+80)+(dim*counter++));
							}else {
								g.drawString("   "+F2[a], headerX, (headerY+80)+(dim*counter++));
							}
						}
						
						if(!value3.isEmpty()) {
							String h3 = WordUtils.wrap(value3, strLength);
							String[] F3 =  h3.split(System.lineSeparator());
							String abc3="";
							for(int a=0; a<F3.length; a++) {
								abc3 += F3[a] + "\n";
								
								if(a==0) {
									g.drawString("• "+F3[0], headerX, (headerY+160)+(dim*counter++));
								}else {
									g.drawString("   "+F3[a], headerX, (headerY+160)+(dim*counter++));
								}
							}
						}
				
						if(!value4.isEmpty()) {
							String h4 = WordUtils.wrap(value4, strLength);
							String[] F4 =  h4.split(System.lineSeparator());
							String abc4="";
							for(int a=0; a<F4.length; a++) {
								abc4 += F4[a] + "\n";
								
								if(a==0) {
									g.drawString("• "+F4[0], headerX, (headerY+240)+(dim*counter++));
								}else {
									g.drawString("   "+F4[a], headerX, (headerY+240)+(dim*counter++));
								}
							}
						}
						
					}else if (obj.getLayout().equalsIgnoreCase(("Features1"))) {
						
						g.drawImage(bufferedImage, 0, 0, null); // bg Canvas
						
						BufferedImage transparentPath = Scalr.resize(transImage2, Method.ULTRA_QUALITY, 1600,110);
						g.drawImage(transparentPath, -600, 1, null); // bg Canvas
						
						g.setColor(Color.BLACK);
						g.setFont(new Font(obj.getFontName(), Font.BOLD, 65));
						
						g.drawString("Features", 120, 200);
						
						g.setColor(Color.BLACK);
						g.setFont(new Font(obj.getFontName(), Font.PLAIN, obj.getFontSize()));
						//g.drawString(value1, 700, 350);
						
						int headerX = 80;
						int headerY = 300;
						
						int strLength = 31;
						
						String h1 = WordUtils.wrap(value1, strLength);
						String[] F1 =  h1.split(System.lineSeparator());
						String abc="";
						int dim = 45;
						int counter= 1;
						for(int a=0; a<F1.length; a++) {
							abc += F1[a] + "\n";
							
							if(a==0) {
								g.drawString("• "+F1[0], headerX, headerY+(dim*counter++));
							}else {
								g.drawString("   "+F1[a], headerX, headerY+(dim*counter++));
							}
						}
						
						String h2 = WordUtils.wrap(value2, strLength);
						String[] F2 =  h2.split(System.lineSeparator());
						String abc2="";
						for(int a=0; a<F2.length; a++) {
							abc2 += F2[a] + "\n";
							
							if(a==0) {
								g.drawString("• "+F2[0], headerX, (headerY+80)+(dim*counter++));
							}else {
								g.drawString("   "+F2[a], headerX, (headerY+80)+(dim*counter++));
							}
						}
						
						if(!value3.isEmpty()) {
							String h3 = WordUtils.wrap(value3, strLength);
							String[] F3 =  h3.split(System.lineSeparator());
							String abc3="";
							for(int a=0; a<F3.length; a++) {
								abc3 += F3[a] + "\n";
								
								if(a==0) {
									g.drawString("• "+F3[0], headerX, (headerY+160)+(dim*counter++));
								}else {
									g.drawString("   "+F3[a], headerX, (headerY+160)+(dim*counter++));
								}
							}
						}
						
						if(!value4.isEmpty()) {
							String h4 = WordUtils.wrap(value4, strLength);
							String[] F4 =  h4.split(System.lineSeparator());
							String abc4="";
							for(int a=0; a<F4.length; a++) {
								abc4 += F4[a] + "\n";
								
								if(a==0) {
									g.drawString("• "+F4[0], headerX, (headerY+240)+(dim*counter++));
								}else {
									g.drawString("   "+F4[a], headerX, (headerY+240)+(dim*counter++));
								}
							}
						}
						
					} else {
						System.out.println("Error Detecting Layer");
					}
					
					/**
					 * Creates Folder if not created
					 */
					File outputFolder = new File(modelConstants.getFeatureCalloutOutputImage()+"/"+batchName);
					//LOGGER.info( obj.getOutput_folder()+"==createFeatureCallImage17"+ obj.getOutputImageName());
					if (!outputFolder.exists()) 
					{				
						outputFolder.mkdirs();
						//LOGGER.info( "!outputFolder.exists()");
					}					
				
					//**************************************Magic Happens Here (Combine and save images)**************************************//

					ImageIO.write(combined, "jpg", new File(modelConstants.getFeatureCalloutOutputImage()+"/"+batchName, obj.getOutputImageName()));
					LOGGER.info("OP Image: "+obj.getOutputImageName()+ " Combined Successfully");
					uploadFile(combined,batchName,obj.getOutputImageName());//Upload Images on AWS S3 
					
				
					}
				}else {
					LOGGER.info("whiteImageFile Read Error Check on this Path Code Line 394---"+modelConstants.getFeatureCalloutBatchImages()+batchName+"/"+"ProductImages/"+obj.getSKU_ID());
				}
				
				message="Feature Callout Image Generated"; 
				
			} // else end, not thumbs.db
			
		} catch (Exception e) {
			LOGGER.info("Exception Error"+e);
			message="Feature Callout Image Generation Failed"; 
			e.printStackTrace();
		}
		
		
	
		return message;
	}

	@SuppressWarnings("unused")
	public static void calculateHorizontalFeatures() {

		int feature1x = margin_leftRight;
		int feature1y = margin_topBottom;
		int feature2x = imgWidth - featureImageWidth - margin_leftRight;
		int feature2y = margin_topBottom;
		int feature3x = margin_leftRight;
		int feature3y = imgHeight - margin_topBottom - featureImageHeight - featureTextdist - featureTextMaxHeight;
		int feature4x = imgWidth - featureImageWidth - margin_leftRight;
		int feature4y = imgHeight - margin_topBottom - featureImageHeight - featureTextdist - featureTextMaxHeight;
		int feature5x = (imgWidth / 2) - (featureImageWidth / 2);
		int feature5y = margin_topBottom;
		int feature6x = (imgWidth / 2) - (featureImageWidth / 2);
		int feature6y = imgHeight - margin_topBottom - featureImageHeight - featureTextdist - featureTextMaxHeight;
	}

	public static void drawCenteredString(Graphics g, String s, Rectangle r, Font font) {
		FontRenderContext frc = new FontRenderContext(null, true, true);

		Rectangle2D r2D = font.getStringBounds(s, frc);
		int rWidth = (int) Math.round(r2D.getWidth());
		int rHeight = (int) Math.round(r2D.getHeight());
		int rX = (int) Math.round(r2D.getX());
		int rY = (int) Math.round(r2D.getY());

		int a = (r.width / 2) - (rWidth / 2) - rX;
		int b = (r.height / 2) - (rHeight / 2) - rY;

		g.setFont(font);
		g.drawString(s, r.x + a, r.y + b);
	}
	
	public static void drawStringMulti(Graphics g, String s, int x, int y, int width)
	{
	    // FontMetrics gives us information about the width,
	    // height, etc. of the current Graphics object's Font.
	    FontMetrics fm = g.getFontMetrics();

	    int lineHeight = fm.getHeight();

	    int curX = x;
	    int curY = y;

	    String[] words = s.split(" ");

	    for (String word : words)
	    {
	        // Find out thw width of the word.
	        int wordWidth = fm.stringWidth(word + " ");

	        // If text exceeds the width, then move to next line.
	        if (curX + wordWidth >= x + width)
	        {
	            curY += lineHeight;
	            curX = x;
	        }

	        g.drawString(word, curX, curY);

	        // Move over to the right for next word.
	        curX += wordWidth;
	    }
	}
	
    public ZipOutputStream zipIt(String zipFile,String SOURCE_FOLDER, List <String> fileList2) {                
        byte[] buffer = new byte[1024];
       // String source = new File(SOURCE_FOLDER).getName();
        FileOutputStream fos = null;
        ZipOutputStream zos2 = null;

        ZipEntry ze=null;
        try {
            FileInputStream in=null;
            fos = new FileOutputStream(zipFile);
            zos2 = new ZipOutputStream(fos);

            for (String file: fileList2) {

               ze = new ZipEntry(file);
                zos2.putNextEntry(ze);
                try {                            
                    in = new FileInputStream(SOURCE_FOLDER + File.separator + file);    

                    int len;
                    while ((len = in .read(buffer)) > 0) {
                        zos2.write(buffer, 0, len);
                    }
                } catch(Exception e) {
                    System.out.print(e);
                }
            }

            zos2.closeEntry();
            LOGGER.info("Folder successfully compressed");
        } catch (IOException ex) {

            ex.printStackTrace();
        } finally {
            try {
                zos2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return zos2;
    }        


    List <String> fileList =new ArrayList < String > ();

    public  List <String> generateFileList(File node,String SOURCE_FOLDER) {

          if (node.isFile()) {                          
              fileList.add(generateZipEntry(node.toString(),SOURCE_FOLDER));                         
          }                    
        if (node.isDirectory()) {                        
            String[] subNote = node.list();                      
            for (String filename: subNote) {

                generateFileList(new File(node, filename),SOURCE_FOLDER);                            
            }                                  
        }
        return fileList;
    }
	
    String generateZipEntry(String file,String SOURCE_FOLDER) {              
        return file.substring(SOURCE_FOLDER.length() + 1, file.length());
      }

	@Override
	public String downloadFeatureImageZip(String clientId, String projectId, String batchName) {
		List<FeatureCalloutDataDTO> batchDetails = featurecalloutrepository.fetchfeatureCalloutBatchData(clientId, projectId,batchName);
		String OUTPUT_ZIP_FILE="";
		 List <String> fileList2=null;
		 
       String SOURCE_FOLDER = "";
       ZipOutputStream zos = null;
		String output_file=modelConstants.getFeatureCalloutOutputImage()+batchName;
		//LOGGER.info("output_file--"+output_file);
		 OUTPUT_ZIP_FILE = output_file+".zip";
		  SOURCE_FOLDER = output_file;
		fileList2=generateFileList(new File(SOURCE_FOLDER),SOURCE_FOLDER);                              
      if(fileList2.isEmpty()==false) 
      {
          zos= zipIt(OUTPUT_ZIP_FILE,SOURCE_FOLDER,fileList2);
          return OUTPUT_ZIP_FILE;
      }
      else 
      {
          return "No Dump Available"; 
      }
		
	}
	
	//Updated By Aadesh For Image Derivation on 23-02-2023

	// @Async annotation ensures that the method is executed in a different background thread 
	// but not consume the main thread.
	@Async
	public void uploadFile(BufferedImage image,String batchName, String imageName) throws Exception {
		//LOGGER.info("File upload in progress.");
		try {
         
                uploadFileToS3Bucket(bucketName,image,batchName,imageName); //updated by sai for image compression on 12-04-2023
		
			  
           
		    } catch (final AmazonServiceException ex) {
			//LOGGER.info("File upload is failed.");
			LOGGER.error("Error= {} while uploading file.", ex.getMessage());
		    }

                
			   // uploadFileToS3Bucket(bucketName, file,folderName);
			  //  LOGGER.info("File upload is completed.");
			   // file.delete();	// To remove the file locally created in the project folder.
            
		     
          
			
	}

	private File convertMultiPartFileToFile(final MultipartFile multipartFile) {
		final File file = new File(multipartFile.getOriginalFilename());
		try (final FileOutputStream outputStream = new FileOutputStream(file)) {
			outputStream.write(multipartFile.getBytes());
		} catch (final IOException ex) {
			LOGGER.error("Error converting the multi-part file to file= ", ex.getMessage());
		}
		return file;
	}
	
private void uploadFileToS3Bucket(final String bucketName,BufferedImage image,String folderName,String imageName) {
	
	 ByteArrayOutputStream baos = new ByteArrayOutputStream();
     try {
		ImageIO.write(image, "jpg", baos);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
     byte[] imageData = baos.toByteArray();
    // final String uniqueFileName = LocalDateTime.now() + "_" + file.getName();
  //  final String uniqueFileName = file.getName();
    String foldername=folderName;
    String k=foldername+"/Output/"+imageName;
  
    // Set object metadata
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(imageData.length);
    metadata.setContentType("image/png"); // Replace with appropriate image type if needed

    // Upload the image to S3
    ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
    final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, k, inputStream, metadata);
    
    
	/*LOGGER.info("Uploading file In k= " + k);
	final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, k, file);*/
	amazonS3.putObject(putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead));
    
}

@Override
public int updateStatus_featuredBatch(String clientId, String projectId,String batchName) {
    Query query = new Query((Criteria.where("clientId").is(clientId).and("projectId").is(projectId).and("batchName").is(batchName)));
    Update update = new Update();
    update.set("catalogStatus", 1);
    UpdateResult result = mongoTemplate.updateFirst(query, update, FeatureCalloutDataDTO.class);
    //LOGGER.info("result: " +result);
    if(result == null)
        return 0;                  
   return 1;
}

@Override
public int updateStatus_featuredImage(String clientId, String projectId,String batchName,String imageSKUID) {
    Query query = new Query((Criteria.where("clientId").is(clientId).and("projectId").is(projectId).and("batchName").is(batchName).and("properties.SKU_ID").is(imageSKUID)));
    Update update = new Update();
    update.set("viewStatus", 1);
    UpdateResult result = mongoTemplate.updateFirst(query, update, FeatureCalloutDataDTO.class);
    //LOGGER.info("result: " +result);
    if(result == null)
        return 0;                  
   return 1;
}

//Added By Aadesh For RPD Creation on 22-08-2023 
public String rpdCreation(FeatureCalloutmageProperties obj,String batchName) throws Exception {
	String message="";		
	try {
		
		//File imgfile = new File(obj.getInput_folder() + obj.getSKU_ID());
		File imgfile = new File(modelConstants.getFeatureCalloutBatchImages()+batchName+"/"+"BackgroundImages/"+obj.getSKU_ID()); ///Get Background Images
		//System.out.println("imgfile: "+imgfile.getName());
		
		if (imgfile.getName().equals("Thumbs.db")) {
			imgfile.delete();
		} else {
			
			imgWidth = obj.getImage_Width();
			imgHeight = obj.getImage_Height();

			margin_topBottom = obj.getTopBottom_Margin();
			margin_leftRight = obj.getLeftRight_Margin();

			centerImageCanvasWidth = obj.getCenterImageCanvasWidth();
			centerImageCanvasHeight = obj.getCenterImageCanvasHeight();

			featureImageWidth = obj.getFeature_Image_Width();
			featureImageHeight = obj.getFeature_Image_Height();

			featureTextdist = obj.getFeature_Text_distance();
			featureTextMaxHeight = obj.getFeature_Text_Max_Height();

			textImgDistance = obj.getText_Img_Distance();

			// ___________________________-----_______________________________//
			
			//BufferedImage bufferedImageBG = ImageIO.read(new File(obj.getBackgroundImagePath()));
			BufferedImage bufferedImageBG = ImageIO.read(new File(modelConstants.getFeatureCalloutBatchImages()+batchName+"/"+"BackgroundImages/"+obj.getBackgroundImageName()));
			
			
			BufferedImage bufferedImage = Scalr.resize(bufferedImageBG, Method.ULTRA_QUALITY, obj.getImage_Width(),
					obj.getImage_Height());

			File whiteImageFile = new File(canvasPath);
			if (!whiteImageFile.exists())
				whiteImageFile.mkdirs();
			ImageIO.write(bufferedImage, "jpg", new File(whiteImageFile, obj.getSKU_ID()));
			
			// create the new image, canvas size is the max. of both image sizes
			int w = Math.max(bufferedImage.getWidth(), bufferedImage.getWidth());
			int h = Math.max(bufferedImage.getHeight(), bufferedImage.getHeight());
			BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			
			Graphics2D g = (Graphics2D) combined.getGraphics();
			
			String header1 = "";
			String value1 = "";
			String value2 = "";
			String value3 = "";
			String value4 = "";
			
			BufferedImage logoImage = null;
			//BufferedImage transImage1 = null;
			//BufferedImage transImage2 = null;
			BufferedImage productImage = null;
			
			try {
				//productImage = ImageIO.read(new File(obj.getBackgroundImageName()));
				productImage = ImageIO.read(new File(modelConstants.getFeatureCalloutBatchImages()+batchName+"/ProductImages/"+obj.getSKU_ID()));
				
			}catch (Exception e) {
				LOGGER.info("Image Break"+e);
			}
			
			try {
				logoImage = ImageIO.read(new File(obj.getLogoImagePath(), obj.getLogoImageName()));
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				//transImage1 = ImageIO.read(new File("E:\\Jaydeep WFH\\Imaging\\Feature Callout\\Flipkart\\WalTop\\Woltop_23rd May\\Input\\1333x120.png"));
			}catch (Exception e) {
				e.getMessage();
			}
			
			try {
				//transImage2 = ImageIO.read(new File("E:\\Jaydeep WFH\\Imaging\\Feature Callout\\Flipkart\\WalTop\\Woltop_23rd May\\Input\\Transparent patch 2.png"));
			}catch (Exception e) {
				e.getMessage();
			}
			
			try {
				header1 = obj.getHeader1();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value1 = obj.getKeyVal1();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value2 = obj.getKeyVal2();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value3 = obj.getKeyVal3();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value4 = obj.getKeyVal4();
			} catch (Exception e) {
				e.getMessage();
			}
			
			
			
			BufferedImage logo = null;
			try {
				logo = Scalr.resize(logoImage, Method.ULTRA_QUALITY, 180, 180);
			}catch (Exception e) {
				e.getMessage();
			}
			
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
			// Harcoded textbox size
			Rectangle rect = new Rectangle();
			rect.width = 1450;
			rect.height = 405;
			
			if (obj.getLayout().equalsIgnoreCase(("Lifestyle Image"))) {
				g.drawImage(bufferedImage, 0, 0, null); // bg Canvas
				g.drawImage(logo, 20, 20, null);
				
				//BufferedImage transparentPath = Scalr.resize(transImage1, Method.ULTRA_QUALITY, 1220,120);
				//g.drawImage(transparentPath, 113, 250, null); // bg Canvas
				
				g.setColor(Color.BLACK);
				//g.setFont(new Font(obj.getFontName(), Font.BOLD, obj.getFontSize()));
				
				//drawCenteredString(g, header1, rect, new Font(obj.getFontName(), Font.BOLD, obj.getFontSize()));
				
				int strLength = 35;
				
				String h1 = WordUtils.wrap(header1, strLength);
				String[] F1 =  h1.split(System.lineSeparator());
				String abc="";
				int dim = 20;
				int counter= 1;
				
				Rectangle rect2 = new Rectangle();
				rect2.width = 1450;
				rect2.height = 550+(dim*counter++);
				
				//(headerY+40)+(dim*counter++)
				
				for(int a=0; a<F1.length; a++) {
					abc += F1[a] + "\n";
					
					if(a==0) {
						drawCenteredString(g, F1[0], rect, new Font(obj.getFontName(), Font.BOLD, obj.getFontSize()));
					}else {
						drawCenteredString(g, F1[a], rect2, new Font(obj.getFontName(), Font.BOLD, obj.getFontSize()));
					}
				}
				
				BufferedImage mainImage = null;
			if(productImage.getHeight()>productImage.getWidth()) {
				
				//mainImage = Scalr.resize(productImage, Method.ULTRA_QUALITY, productImage.getWidth() + 100, productImage.getHeight());
				mainImage = Scalr.resize(productImage, Method.ULTRA_QUALITY, 600, 600);
			}else {
				mainImage = Scalr.resize(productImage, Method.ULTRA_QUALITY, 750, 750);
			}
				
				g.drawImage(mainImage,
						centerCalculationX(mainImage.getWidth(), obj.getCenterImageCanvasWidth(),obj.getImage_Width()),
						centerCalculationY(obj.getTopBottom_Margin(), mainImage.getHeight(),obj.getCenterImageCanvasHeight()) + 150,
						null);
				
				
			}else if (obj.getLayout().equalsIgnoreCase(("Feature 1"))) {
				g.drawImage(bufferedImage, 0, 0, null); // bg Canvas
				g.drawImage(logo, 20, 20, null);
				
				//BufferedImage transparentPath = Scalr.resize(transImage2, Method.ULTRA_QUALITY, 1000,110);
				//g.drawImage(transparentPath, -250, 250, null); // bg Canvas
				
				g.setColor(Color.BLACK);
				g.setFont(new Font(obj.getFontName(), Font.BOLD, 40));
				g.drawString(header1, 200, 350);
				
				g.setColor(Color.BLACK);
				g.setFont(new Font(obj.getFontName(), Font.PLAIN, obj.getFontSize()));
				//g.drawString(value1, 700, 350);
				
				BufferedImage mainImage = null;
				if(productImage.getHeight()>productImage.getWidth()) {
					mainImage = Scalr.resize(productImage, Method.ULTRA_QUALITY, 650, 650);
					g.drawImage(mainImage,
							centerCalculationX(mainImage.getWidth(), obj.getCenterImageCanvasWidth(),obj.getImage_Width()) + 300,
							centerCalculationY(obj.getTopBottom_Margin(), mainImage.getHeight(),obj.getCenterImageCanvasHeight()) + 150,
							null);
				}else {
					mainImage = Scalr.resize(productImage, Method.ULTRA_QUALITY, 620, 620);
					
					g.drawImage(mainImage,
							centerCalculationX(mainImage.getWidth(), obj.getCenterImageCanvasWidth(),obj.getImage_Width()) + 300,
							centerCalculationY(obj.getTopBottom_Margin(), mainImage.getHeight(),obj.getCenterImageCanvasHeight()) + 150,
							null);
				}
				
				
				
				int headerX = 180;
				int headerY = 400;
				
				int strLength = 28;
				
				String h1 = WordUtils.wrap(value1, strLength);
				String[] F1 =  h1.split(System.lineSeparator());
				String abc="";
				int dim = 35;
				int counter= 1;
				for(int a=0; a<F1.length; a++) {
					abc += F1[a] + "\n";
					
					if(a==0) {
						g.drawString("• "+F1[0], headerX, headerY+(dim*counter++));
					}else {
						g.drawString("   "+F1[a], headerX, headerY+(dim*counter++));
					}
				}
				
				String h2 = WordUtils.wrap(value2, strLength);
				String[] F2 =  h2.split(System.lineSeparator());
				String abc2="";
				for(int a=0; a<F2.length; a++) {
					abc2 += F2[a] + "\n";
					
					if(a==0) {
						g.drawString("• "+F2[0], headerX, (headerY+40)+(dim*counter++));
					}else {
						g.drawString("   "+F2[a], headerX, (headerY+40)+(dim*counter++));
					}
				}
				
				if(!value3.isEmpty()) {
					String h3 = WordUtils.wrap(value3, strLength);
					String[] F3 =  h3.split(System.lineSeparator());
					String abc3="";
					for(int a=0; a<F3.length; a++) {
						abc3 += F3[a] + "\n";
						
						if(a==0) {
							g.drawString("• "+F3[0], headerX, (headerY+80)+(dim*counter++));
						}else {
							g.drawString("   "+F3[a], headerX, (headerY+80)+(dim*counter++));
						}
					}
				}
				
				if(!value4.isEmpty()) {
					String h4 = WordUtils.wrap(value4, strLength);
					String[] F4 =  h4.split(System.lineSeparator());
					String abc4="";
					for(int a=0; a<F4.length; a++) {
						abc4 += F4[a] + "\n";
						
						if(a==0) {
							g.drawString("• "+F4[0], headerX, (headerY+120)+(dim*counter++));
						}else {
							g.drawString("   "+F4[a], headerX, (headerY+120)+(dim*counter++));
						}
					}
				}
				
			}
			else if (obj.getLayout().equalsIgnoreCase(("Feature 2"))) {
				g.drawImage(bufferedImage, 0, 0, null); // bg Canvas
				g.drawImage(logo, 20, 20, null);
				
				//BufferedImage transparentPath = Scalr.resize(transImage2, Method.ULTRA_QUALITY, 1000,110);
				//g.drawImage(transparentPath, 350, 250, null); // bg Canvas
				
				g.setColor(Color.BLACK);
				g.setFont(new Font(obj.getFontName(), Font.BOLD, 40));
				g.drawString(header1, 800, 350);
				
				g.setColor(Color.BLACK);
				g.setFont(new Font(obj.getFontName(), Font.PLAIN, obj.getFontSize()));
				
				BufferedImage mainImage = null;
				if(productImage.getHeight()>productImage.getWidth()) {
					mainImage = Scalr.resize(productImage, Method.ULTRA_QUALITY, 700, 700);
					g.drawImage(mainImage,
							centerCalculationX(mainImage.getWidth(), obj.getCenterImageCanvasWidth(),obj.getImage_Width()) - 300,
							centerCalculationY(obj.getTopBottom_Margin(), mainImage.getHeight(),obj.getCenterImageCanvasHeight()) + 150,
							null);
				} else {
					mainImage = Scalr.resize(productImage, Method.ULTRA_QUALITY, 650, 650);
					
					g.drawImage(mainImage,
							centerCalculationX(mainImage.getWidth(), obj.getCenterImageCanvasWidth(),obj.getImage_Width()) - 300,
							centerCalculationY(obj.getTopBottom_Margin(), mainImage.getHeight(),obj.getCenterImageCanvasHeight()) + 150,
							null);
				}
				
				
				int headerX = 780;
				int headerY = 400;
				
				int strLength = 28;
				
				String h1 = WordUtils.wrap(value1, strLength);
				String[] F1 =  h1.split(System.lineSeparator());
				String abc="";
				int dim = 35;
				int counter= 1;
				for(int a=0; a<F1.length; a++) {
					abc += F1[a] + "\n";
					
					if(a==0) {
						g.drawString("• "+F1[0], headerX, headerY+(dim*counter++));
					}else {
						g.drawString("   "+F1[a], headerX, headerY+(dim*counter++));
					}
				}
				
				String h2 = WordUtils.wrap(value2, strLength);
				String[] F2 =  h2.split(System.lineSeparator());
				String abc2="";
				for(int a=0; a<F2.length; a++) {
					abc2 += F2[a] + "\n";
					
					if(a==0) {
						g.drawString("• "+F2[0], headerX, (headerY+40)+(dim*counter++));
					}else {
						g.drawString("   "+F2[a], headerX, (headerY+40)+(dim*counter++));
					}
				}
				
				if(!value3.isEmpty()) {
					String h3 = WordUtils.wrap(value3, strLength);
					String[] F3 =  h3.split(System.lineSeparator());
					String abc3="";
					for(int a=0; a<F3.length; a++) {
						abc3 += F3[a] + "\n";
						
						if(a==0) {
							g.drawString("• "+F3[0], headerX, (headerY+80)+(dim*counter++));
						}else {
							g.drawString("   "+F3[a], headerX, (headerY+80)+(dim*counter++));
						}
					}
				}
				
				if(!value4.isEmpty()) {
					String h4 = WordUtils.wrap(value4, strLength);
					String[] F4 =  h4.split(System.lineSeparator());
					String abc4="";
					for(int a=0; a<F4.length; a++) {
						abc4 += F4[a] + "\n";
						
						if(a==0) {
							g.drawString("• "+F4[0], headerX, (headerY+120)+(dim*counter++));
						}else {
							g.drawString("   "+F4[a], headerX, (headerY+120)+(dim*counter++));
						}
					}
				}
				
			} else {
				System.out.println("Error Detecting Layer");
			}
			
			/**
			 * Creates Folder if not created
			 */
			//File outputFolder = new File(obj.getOutput_folder());
			File outputFolder = new File(modelConstants.getFeatureCalloutOutputImage()+"/"+batchName);
			if (!outputFolder.exists())
				outputFolder.mkdirs();
			
			// ************************Magic Happens Here (Combine and save images)**************************************//
			
			LOGGER.info("imgfile: "+obj.getOutputImageName());
			//ImageIO.write(combined, "jpg", new File(obj.getOutput_folder(), obj.getOutputImageName()));
			ImageIO.write(combined, "jpg", new File(modelConstants.getFeatureCalloutOutputImage()+"/"+batchName, obj.getOutputImageName()));
			uploadFile(combined,batchName,obj.getOutputImageName());//Upload Images on AWS S3 
			//System.out.println("---->Image SKU: " + obj.getSKU_ID() +" --- OP Image: "+obj.getOutputImageName()+ " Combined Successfully");
			message="Feature Callout Image Generated"; 
		} // else end, not thumbs.db
	} catch (Exception e) {
		LOGGER.info("Exception Error"+e);
		e.printStackTrace();
	}
	return message;
}

/** * Tatacliq Infographics Integration* * @author  Aadesh Dudhane * @version 1.0 * @since  21-11-2023 */
public String tataInfographics(FeatureCalloutmageProperties obj, String batchName) throws Exception {
	String message="";		
	try {
		
		//File imgfile = new File(obj.getInput_folder() + obj.getSKU_ID());
		File imgfile = new File(modelConstants.getFeatureCalloutBatchImages()+batchName+"/"+"BackgroundImages/"+obj.getSKU_ID()); ///Get Background Images
		//System.out.println("imgfile: "+imgfile.getName());
		
		if (imgfile.getName().equals("Thumbs.db")) {
			imgfile.delete();
		} else {
			
			imgWidth = obj.getImage_Width();
			imgHeight = obj.getImage_Height();
			margin_topBottom = obj.getTopBottom_Margin();
			margin_leftRight = obj.getLeftRight_Margin();
			centerImageCanvasWidth = obj.getCenterImageCanvasWidth();
			centerImageCanvasHeight = obj.getCenterImageCanvasHeight();
			featureImageWidth = obj.getFeature_Image_Width();
			featureImageHeight = obj.getFeature_Image_Height();
			featureTextdist = obj.getFeature_Text_distance();
			featureTextMaxHeight = obj.getFeature_Text_Max_Height();
			textImgDistance = obj.getText_Img_Distance();
			// ___________________________-----_______________________________//
			
			//BufferedImage bufferedImage = ImageIO.read(new File(obj.getBackgroundImagePath()+obj.getBackgroundImageName()));
			BufferedImage bufferedImageBG = ImageIO.read(new File(modelConstants.getFeatureCalloutBatchImages()+batchName+"/"+"BackgroundImages/"+obj.getBackgroundImageName()));
			BufferedImage bufferedImage = Scalr.resize(bufferedImageBG, Method.ULTRA_QUALITY, obj.getImage_Width(),
					obj.getImage_Height());
			//System.out.println(bufferedImage.toString());
			//BufferedImage bufferedImage = Scalr.resize(bufferedImageBG, Method.ULTRA_QUALITY, obj.getImage_Width(), obj.getImage_Height());
			
			File whiteImageFile = new File(canvasPath);
			if (!whiteImageFile.exists())
				whiteImageFile.mkdirs();
			ImageIO.write(bufferedImage, "png", new File(whiteImageFile, obj.getSKU_ID()));
			
			// create the new image, canvas size is the max. of both image sizes
			int w = Math.max(bufferedImage.getWidth(), bufferedImage.getWidth());
			int h = Math.max(bufferedImage.getHeight(), bufferedImage.getHeight());
			BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			
			Graphics2D g = (Graphics2D) combined.getGraphics();
			
			String header1 = "";
			String value1 = "";
			
			String header2 = "";
			String value2 = "";
			
			String header3 = "";
			String value3 = "";
			
			String header4 = "";
			String value4 = "";
			
			String header5 = "";
			String value5 = "";
			
			String header6 = "";
			String value6 = "";
			
			BufferedImage fullTransparentBackground = null;
			BufferedImage rightPatch = null;
			BufferedImage objPatch = null;
			
			try {
				fullTransparentBackground = ImageIO.read(new File(modelConstants.getFeatireCalloutSupportImage()+"LargeBackground.png"));
			}catch (Exception e) {
				e.getMessage();
			}
			
			try {
				rightPatch = ImageIO.read(new File(modelConstants.getFeatireCalloutSupportImage()+"RightPatch.png"));
			}catch (Exception e) {
				e.getMessage();
			}
			
			try {
				objPatch = ImageIO.read(new File(modelConstants.getFeatireCalloutSupportImage()+"icon.png"));
			}catch (Exception e) {
				e.getMessage();
			}
			
			try {
				header1 = obj.getHeader1();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				header2 = obj.getHeader2();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				header3 = obj.getHeader3();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				header4 = obj.getHeader4();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				header5 = obj.getHeader5();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				header6 = obj.getHeader6();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value1 = obj.getKeyVal1();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value2 = obj.getKeyVal2();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value3 = obj.getKeyVal3();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value4 = obj.getKeyVal4();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value5 = obj.getKeyVal5();
			} catch (Exception e) {
				e.getMessage();
			}
			
			try {
				value6 = obj.getKeyVal6();
			} catch (Exception e) {
				e.getMessage();
			}
			
			// Harcoded textbox size
			
			
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
			
			if (obj.getLayout().equalsIgnoreCase(("Feature 1"))) {
				
				g.drawImage(bufferedImage, 0, 0, null);
				g.drawImage(fullTransparentBackground, 0, 0, null);
				g.drawImage(rightPatch, 760, 0, null);
				
				BufferedImage objPatchImage = Scalr.resize(objPatch, Method.ULTRA_QUALITY, 58,57);
				
				if(obj.getFeature_Count()==4) {
					
					g.drawImage(objPatchImage, 730, 390, null);
					g.drawImage(objPatchImage, 730, 540, null);
					g.drawImage(objPatchImage, 730, 690, null);
					g.drawImage(objPatchImage, 730, 840, null);
					
					g.setColor(Color.GRAY);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 30));
					
					g.drawString(header1, 800, 410);
					g.drawString(header2, 800, 560);
					g.drawString(header3, 800, 710);
					g.drawString(header4, 800, 860);
					
					g.setColor(Color.WHITE);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 34));
					g.drawString(value1, 800, 450);
					g.drawString(value2, 800, 600);
					g.drawString(value3, 800, 750);
					g.drawString(value4, 800, 900);
					
				}else if(obj.getFeature_Count()==5) {
					
					g.drawImage(objPatchImage, 730, 300, null);
					g.drawImage(objPatchImage, 730, 450, null);
					g.drawImage(objPatchImage, 730, 600, null);
					g.drawImage(objPatchImage, 730, 750, null);
					g.drawImage(objPatchImage, 730, 900, null);
					
					g.setColor(Color.GRAY);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 30));
					
					g.drawString(header1, 800, 310);
					g.drawString(header2, 800, 460);
					g.drawString(header3, 800, 610);
					g.drawString(header4, 800, 760);
					g.drawString(header5, 800, 910);
					
					g.setColor(Color.WHITE);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 34));
					g.drawString(value1, 800, 350);
					g.drawString(value2, 800, 500);
					g.drawString(value3, 800, 650);
					g.drawString(value4, 800, 800);
					g.drawString(value5, 800, 950);
					
				}else if(obj.getFeature_Count()==6) {
					
					g.drawImage(objPatchImage, 730, 200, null);
					g.drawImage(objPatchImage, 730, 380, null);
					g.drawImage(objPatchImage, 730, 560, null);
					g.drawImage(objPatchImage, 730, 740, null);
					g.drawImage(objPatchImage, 730, 920, null);
					g.drawImage(objPatchImage, 730, 1100, null);
					
					g.setColor(Color.GRAY);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 30));
					
					g.drawString(header1, 800, 210);
					g.drawString(header2, 800, 390);
					g.drawString(header3, 800, 570);
					g.drawString(header4, 800, 750);
					g.drawString(header5, 800, 930);
					g.drawString(header6, 800, 1110);
					
					g.setColor(Color.WHITE);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 34));
					g.drawString(value1, 800, 250);
					g.drawString(value2, 800, 440);
					g.drawString(value3, 800, 610);
					g.drawString(value4, 800, 790);
					g.drawString(value5, 800, 970);
					g.drawString(value6, 800, 1150);	
				}
				
			}else if (obj.getLayout().equalsIgnoreCase(("Feature 2"))) {
				
				g.drawImage(bufferedImage, 0, 0, null);
				g.drawImage(fullTransparentBackground, 0, 0, null);
				g.drawImage(rightPatch, 760, 0, null);
				
				BufferedImage objPatchImage = Scalr.resize(objPatch, Method.ULTRA_QUALITY, 58,57);
				
				if(obj.getFeature_Count()==4) {
					
					g.drawImage(objPatchImage, 730, 390, null);
					g.drawImage(objPatchImage, 730, 540, null);
					g.drawImage(objPatchImage, 730, 690, null);
					g.drawImage(objPatchImage, 730, 840, null);
					
					g.setColor(Color.GRAY);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 30));
					
					g.drawString(header1, 800, 410);
					g.drawString(header2, 800, 560);
					g.drawString(header3, 800, 710);
					g.drawString(header4, 800, 860);
					
					g.setColor(Color.WHITE);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 34));
					g.drawString(value1, 800, 450);
					g.drawString(value2, 800, 600);
					g.drawString(value3, 800, 750);
					g.drawString(value4, 800, 900);
					
				}else if(obj.getFeature_Count()==5) {
					
					g.drawImage(objPatchImage, 730, 300, null);
					g.drawImage(objPatchImage, 730, 450, null);
					g.drawImage(objPatchImage, 730, 600, null);
					g.drawImage(objPatchImage, 730, 750, null);
					g.drawImage(objPatchImage, 730, 900, null);
					
					g.setColor(Color.GRAY);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 30));
					
					g.drawString(header1, 800, 310);
					g.drawString(header2, 800, 460);
					g.drawString(header3, 800, 610);
					g.drawString(header4, 800, 760);
					g.drawString(header5, 800, 910);
					
					g.setColor(Color.WHITE);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 34));
					g.drawString(value1, 800, 350);
					g.drawString(value2, 800, 500);
					g.drawString(value3, 800, 650);
					g.drawString(value4, 800, 800);
					g.drawString(value5, 800, 950);
					
				}else if(obj.getFeature_Count()==6) {
					
					g.drawImage(objPatchImage, 730, 200, null);
					g.drawImage(objPatchImage, 730, 380, null);
					g.drawImage(objPatchImage, 730, 560, null);
					g.drawImage(objPatchImage, 730, 740, null);
					g.drawImage(objPatchImage, 730, 920, null);
					g.drawImage(objPatchImage, 730, 1100, null);
					
					g.setColor(Color.GRAY);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 30));
					
					g.drawString(header1, 800, 210);
					g.drawString(header2, 800, 390);
					g.drawString(header3, 800, 570);
					g.drawString(header4, 800, 750);
					g.drawString(header5, 800, 930);
					g.drawString(header6, 800, 1110);
					
					g.setColor(Color.WHITE);
					g.setFont(new Font(obj.getFontName(), Font.PLAIN, 34));
					g.drawString(value1, 800, 250);
					g.drawString(value2, 800, 440);
					g.drawString(value3, 800, 610);
					g.drawString(value4, 800, 790);
					g.drawString(value5, 800, 970);
					g.drawString(value6, 800, 1150);	
				}
				
			}
			else {
				LOGGER.info("Error Detecting Layer");
			}		
			/**
			 * Creates Folder if not created
			 */
			//File outputFolder = new File(obj.getOutput_folder());
			File outputFolder = new File(modelConstants.getFeatureCalloutOutputImage()+"/"+batchName);
			if (!outputFolder.exists())
				outputFolder.mkdirs();
			
			// ************************Magic Happens Here (Combine and save images)**************************************//
			
			LOGGER.info("imgfile: "+obj.getOutputImageName());
			//ImageIO.write(combined, "jpg", new File(obj.getOutput_folder(), obj.getOutputImageName()));
			ImageIO.write(combined, "jpg", new File(modelConstants.getFeatureCalloutOutputImage()+"/"+batchName, obj.getOutputImageName()));
			uploadFile(combined,batchName,obj.getOutputImageName());//Upload Images on AWS S3 
			message="Feature Callout Image Generated"; 
			
			
		} // else end, not thumbs.db
	} catch (Exception e) {
		LOGGER.info("Exception Error"+e);
		e.printStackTrace();
	}
	return message;
}
}