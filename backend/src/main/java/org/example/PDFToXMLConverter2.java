package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.text.PDFTextStripper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PDFToXMLConverter2 {
    public static void convertPDFToXML(String pdfPath, String xmlOutputPath, String imageOutputDir) {
        try {
            File pdfFile = new File(pdfPath);
            PDDocument document = PDDocument.load(pdfFile);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            PDDocumentInformation info = document.getDocumentInformation();
            File imageDir = new File(imageOutputDir);
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }
            Map<Integer, List<String>> pageImages = extractEmbeddedImages(document, imageDir);
            Document xmlDocument = convertToXML(document, text, pageImages, info, pdfPath);
            saveXML(xmlDocument, xmlOutputPath);
            document.close();
            System.out.println("PDF converted to XML successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    } 

    private static void addMetadataElement(Document doc, Element parent, String tagName, String value) {
    if (value != null && !value.isEmpty()) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(value));
        parent.appendChild(element);
    }
    }

    private static Map<Integer, List<String>> extractEmbeddedImages(PDDocument document, File outputDir) throws IOException {
        Map<Integer, List<String>> pageImages = new HashMap<>();
        int pageIndex = 0;
        for (PDPage page : document.getPages()) {
            pageIndex++;
            PDResources resources = page.getResources();
            List<String> imagePaths = new ArrayList<>();
            for (COSName xObjectName : resources.getXObjectNames()) {
                if (resources.isImageXObject(xObjectName)) {
                    PDImageXObject image = (PDImageXObject) resources.getXObject(xObjectName);
                    BufferedImage bufferedImage = image.getImage();
                    File outputFile = new File(outputDir, "embedded_image_" + pageIndex + "_" + imagePaths.size() + ".png");
                    ImageIO.write(bufferedImage, "png", outputFile);
                    imagePaths.add(outputFile.getAbsolutePath());
                    System.out.println("Extracted embedded image: " + outputFile.getAbsolutePath());
                }
            }
            if (!imagePaths.isEmpty()) {
                pageImages.put(pageIndex, imagePaths);
            }
        }
        return pageImages;
    }

    

    private static String formatFileSize(long size) {
    if (size < 1024) return size + " B";
    int exp = (int) (Math.log(size) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "B";
    return String.format("%.1f %s", size / Math.pow(1024, exp), pre);
}


 private static Document convertToXML(PDDocument document, String text, Map<Integer, List<String>> pageImages, PDDocumentInformation info, String pdfFilePath) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();

    Element root = doc.createElement("book");
    doc.appendChild(root);
    Element metadata = doc.createElement("metadata");
    root.appendChild(metadata);
    File pdfFile = new File(pdfFilePath);
    long fileSizeBytes = pdfFile.length();
    String fileSizeFormatted = formatFileSize(fileSizeBytes);

    addMetadataElement(doc, metadata, "file_size", fileSizeFormatted);
    addMetadataElement(doc, metadata, "title", info.getTitle());
    addMetadataElement(doc, metadata, "author", info.getAuthor());
    addMetadataElement(doc, metadata, "subject", info.getSubject());
    addMetadataElement(doc, metadata, "keywords", info.getKeywords());
    addMetadataElement(doc, metadata, "creator", info.getCreator());
    addMetadataElement(doc, metadata, "producer", info.getProducer());
    addMetadataElement(doc, metadata, "creation_date", (info.getCreationDate() != null) ? info.getCreationDate().toString() : null);
    addMetadataElement(doc, metadata, "modification_date", (info.getModificationDate() != null) ? info.getModificationDate().toString() : null);
    addMetadataElement(doc, metadata, "file_size", fileSizeFormatted);

    String[] lines = text.split("\n");
    Element currentChapter = null;
    Element currentSection = null;

    int currentPage = 1;
    Map<Integer, List<String>> pageText = new HashMap<>();
    int pageNum = 1;
    
    // Organize text into pages (assuming "---Page X---" is a delimiter in the text)
    PDFTextStripper textStripper = new PDFTextStripper();


    for (PDPage page : document.getPages()) {
        textStripper.setStartPage(pageNum);
        textStripper.setEndPage(pageNum);

        String text1 = textStripper.getText(document).trim();
        List<String> currentPageLines = Arrays.asList(text1.split("\\r?\\n"));

        if (!currentPageLines.isEmpty()) {
            pageText.put(pageNum, new ArrayList<>(currentPageLines));
        }

        pageNum++; // Move to the next page
    }
    StringBuilder paragraphContent = new StringBuilder();
    


    for (; pageText.containsKey(currentPage); currentPage++) {
        List<String> pageLines = pageText.get(currentPage);
        
        for (String line : pageLines) {
            line = line.trim();

            // Detect chapter titles
            if (line.startsWith("Chapter")) {
                // Save previous paragraph if exists
                if (paragraphContent.length() > 0) {
                    Element paragraph = doc.createElement("paragraph");
                    paragraph.appendChild(doc.createTextNode(paragraphContent.toString().trim()));
                    if (currentSection != null) {
                        currentSection.appendChild(paragraph);
                    } else if (currentChapter != null) {
                        currentChapter.appendChild(paragraph);
                    } else {
                        root.appendChild(paragraph);
                    }
                    paragraphContent.setLength(0);
                }

                currentChapter = doc.createElement("chapter");
                currentChapter.setAttribute("title", line);
                root.appendChild(currentChapter);
                currentSection = null;
            }
            // Detect section titles (e.g., 1.1: Introduction)
            else if (line.matches("^\\d+\\.\\d+: .*")) {
                // Save previous paragraph if exists
                if (paragraphContent.length() > 0) {
                    Element paragraph = doc.createElement("paragraph");
                    paragraph.appendChild(doc.createTextNode(paragraphContent.toString().trim()));
                    if (currentSection != null) {
                        currentSection.appendChild(paragraph);
                    } else if (currentChapter != null) {
                        currentChapter.appendChild(paragraph);
                    } else {
                        root.appendChild(paragraph);
                    }
                    paragraphContent.setLength(0);
                }

                currentSection = doc.createElement("section");
                currentSection.setAttribute("title", line);
                if (currentChapter != null) {
                    currentChapter.appendChild(currentSection);
                }
            }
            // Detect image captions
            else if (line.matches("(?i)^(Figure|Image)\\s*\\d+.*")) {
                if (paragraphContent.length() > 0) {
                    Element paragraph = doc.createElement("paragraph");
                    paragraph.appendChild(doc.createTextNode(paragraphContent.toString().trim()));
                    if (currentSection != null) {
                        currentSection.appendChild(paragraph);
                    } else if (currentChapter != null) {
                        currentChapter.appendChild(paragraph);
                    } else {
                        root.appendChild(paragraph);
                    }
                    paragraphContent.setLength(0);
                }

                Element caption = doc.createElement("image");
                caption.appendChild(doc.createTextNode(line));

                if (currentSection != null) {
                    currentSection.appendChild(caption);
                } else if (currentChapter != null) {
                    currentChapter.appendChild(caption);
                } else {
                    root.appendChild(caption);
                }

                if (pageImages.containsKey(currentPage) && !pageImages.get(currentPage).isEmpty()) {
                    String imgPath = pageImages.get(currentPage).remove(0); 

                    Element imgElement = doc.createElement("image");
                    imgElement.setAttribute("src", imgPath);

                    if (currentSection != null) {
                        currentSection.appendChild(imgElement);
                    } else if (currentChapter != null) {
                        currentChapter.appendChild(imgElement);
                    } else {
                        root.appendChild(imgElement);
                    }
                }
            }
            // Accumulate lines into paragraphs
            else if (!line.isEmpty()) {
                if (paragraphContent.length() > 0) {
                    paragraphContent.append(" ");
                }
                paragraphContent.append(line);
            }
        }
    
        
    }

    // Save last paragraph if exists
    if (paragraphContent.length() > 0) {
        Element paragraph = doc.createElement("paragraph");
        paragraph.appendChild(doc.createTextNode(paragraphContent.toString().trim()));
        if (currentSection != null) {
            currentSection.appendChild(paragraph);
        } else if (currentChapter != null) {
            currentChapter.appendChild(paragraph);
        } else {
            root.appendChild(paragraph);
        }
    }

    
    return doc;
}


    private static void saveXML(Document doc, String filePath) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));
        transformer.transform(source, result);
    }
}