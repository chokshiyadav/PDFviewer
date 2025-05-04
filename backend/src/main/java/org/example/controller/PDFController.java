package org.example.controller;

import org.example.PDFToXMLConverter2;
import org.example.XmlToPdf;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.*;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class PDFController {

private static final String UPLOAD_DIR = System.getProperty("java.io.tmpdir") + "/pdfuploads/";

@PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<String> convertPdfToXml(@RequestParam("pdf") MultipartFile file) {
    if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("No file uploaded");
    }

    try {
        // Ensure upload directory exists
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Save uploaded PDF to temp file
        Path pdfPath = Paths.get(UPLOAD_DIR, file.getOriginalFilename());
        Files.write(pdfPath, file.getBytes());

        // Define output XML path
        String xmlOutputPath = UPLOAD_DIR + "output.xml";
        String imageOutputDir = UPLOAD_DIR + "images/";

        // Convert PDF to XML using provided converter
        PDFToXMLConverter2.convertPDFToXML(pdfPath.toString(), xmlOutputPath, imageOutputDir);

        // Read the generated XML content
        String xmlContent = Files.readString(Paths.get(xmlOutputPath));

        // Clean up uploaded files if needed (optional)

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xmlContent);

    } catch (IOException e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file");
    }
}

@PostMapping(value = "/export", consumes = MediaType.APPLICATION_XML_VALUE)
public ResponseEntity<?> exportXmlToPdf(@RequestBody String xmlContent) {
    try {
        // Ensure upload directory exists
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Save XML content to file
        String xmlFilePath = UPLOAD_DIR + "output.xml";
        Files.writeString(Paths.get(xmlFilePath), xmlContent);

        // Define output PDF path
        String pdfOutputPath = UPLOAD_DIR + "output.pdf";

        // Path to XSL stylesheet
        String xslFilePath = "src/main/resources/annotate.xsl";

        // Convert XML to PDF
        XmlToPdf.convertXmlToPdf(xmlFilePath, xslFilePath, pdfOutputPath);

        // Return PDF as response
        InputStreamResource resource = new InputStreamResource(new FileInputStream(pdfOutputPath));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(new File(pdfOutputPath).length())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error exporting PDF");
    }
}

@PostMapping(value = "/exportToHtml")
public ResponseEntity<?> exportToHtml() {
    try {
        File xmlFile = new File(UPLOAD_DIR + "output.xml");
        if (!xmlFile.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("XML file not found");
        }

        File xslFile = new File("src/main/resources/html.xsl");
        if (!xslFile.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("XSL file not found");
        }

        File htmlFile = new File(UPLOAD_DIR + "output.html");
        OutputStream htmlOutputStream = new FileOutputStream(htmlFile);

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(xslFile));
        transformer.transform(new StreamSource(xmlFile), new StreamResult(htmlOutputStream));

        htmlOutputStream.close();

        InputStreamResource resource = new InputStreamResource(new FileInputStream(htmlFile));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.html");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(htmlFile.length())
                .contentType(MediaType.TEXT_HTML)
                .body(resource);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error exporting HTML");
    }
}

    @PostMapping(value = "/exportTranslated", consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> exportTranslatedXmlToPdf(@RequestBody String xmlContent) {
        try {
            // Ensure upload directory exists
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Save XML content to file
            String xmlFilePath = UPLOAD_DIR + "output.xml";
            Files.writeString(Paths.get(xmlFilePath), xmlContent);

            // Define translated XML and PDF paths
            String translatedXmlPath = UPLOAD_DIR + "translate.xml";
            String pdfOutputPath = UPLOAD_DIR + "translated_output.pdf";

            // Translate XML to multilingual XML
            org.example.TranslateXML.convertToMultiLingualXML(xmlFilePath, translatedXmlPath);

            // Path to translate.xsl and fop.xconf
            String xslFilePath = "src/main/resources/translate.xsl";
            String fopConfigPath = "src/main/resources/fop.xconf";

            // Run Apache FOP command to generate PDF
            ProcessBuilder pb = new ProcessBuilder(
                "fop",
                "-c", fopConfigPath,
                "-xml", translatedXmlPath,
                "-xsl", xslFilePath,
                "-pdf", pdfOutputPath
            );

            pb.inheritIO(); // Optional: to see the output/errors in your console

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture output for debugging
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating translated PDF");
            }

            // Return translated PDF as response
            File pdfFile = new File(pdfOutputPath);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(pdfFile));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translated_output.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(pdfFile.length())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error exporting translated PDF");
        }
    }

    @PostMapping(value = "/annotate", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addAnnotation(@RequestBody String annotationText) {
        try {
            File xmlFile = new File(UPLOAD_DIR + "output.xml");
            if (!xmlFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("XML file not found");
            }

            String xmlContent = Files.readString(xmlFile.toPath());

            String annotationTag = "<annotation>" + annotationText + "</annotation>";

            // Insert annotation before closing </book> tag
            int insertPos = xmlContent.lastIndexOf("</book>");
            if (insertPos == -1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid XML format: missing </book> tag");
            }

            String updatedXml = xmlContent.substring(0, insertPos) + annotationTag + xmlContent.substring(insertPos);

            Files.writeString(xmlFile.toPath(), updatedXml);

            return ResponseEntity.ok("Annotation added successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding annotation");
        }
    }

    // DTO class for annotation request
    public static class AnnotationRequest {
        private String type;
        private String text;
        private String url;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    @GetMapping(value = "/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getUpdatedXml() {
        try {
            File xmlFile = new File(UPLOAD_DIR + "output.xml");
            if (!xmlFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("XML file not found");
            }
            String xmlContent = Files.readString(xmlFile.toPath());
            return ResponseEntity.ok(xmlContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading XML file");
        }
    }

    @PostMapping(value = "/exportChapter")
    public ResponseEntity<?> exportChapter(@RequestBody String chapterTitle) {
        try {
            File xmlFile = new File(UPLOAD_DIR + "output.xml");
            if (!xmlFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("XML file not found");
            }

            // Load the XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);

            NodeList chapters = document.getElementsByTagName("chapter");
            boolean found = false;

            for (int i = 0; i < chapters.getLength(); i++) {
                Element chapter = (Element) chapters.item(i);
                String title = chapter.getAttribute("title");

                if (chapterTitle.equals(title)) {
                    // Save to a new file with <book> as root
                    saveChapterToFile(chapter, chapterTitle);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chapter with title '" + chapterTitle + "' not found.");
            }
            
            // After saving XML, generate translated PDF using existing logic
            String translatedXmlPath = UPLOAD_DIR + chapterTitle + "_translated.xml";
            String pdfOutputPath = UPLOAD_DIR + chapterTitle + "_translated.pdf";

            org.example.TranslateXML.convertToMultiLingualXML(translatedXmlPath, translatedXmlPath);

            // Path to translate.xsl and fop.xconf
            String xslFilePath = "src/main/resources/translate.xsl";
            String fopConfigPath = "src/main/resources/fop.xconf";

            // Run Apache FOP command to generate PDF
            ProcessBuilder pb = new ProcessBuilder(
                "fop",
                "-c", fopConfigPath,
                "-xml", translatedXmlPath,
                "-xsl", xslFilePath,
                "-pdf", pdfOutputPath
            );

            pb.inheritIO(); // Optional: to see the output/errors in your console

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture output for debugging
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating translated PDF");
            }

            // Return the PDF file as response for download
            File pdfFile = new File(pdfOutputPath);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(pdfFile));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + chapterTitle + "_translated.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(pdfFile.length())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error exporting chapter and generating PDF");
        }
    }

    private void saveChapterToFile(Node chapter, String chapterTitle) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDocument = builder.newDocument();

        // Create <book> root
        Element bookElement = newDocument.createElement("book");
        newDocument.appendChild(bookElement);

        // Import the chapter node inside book
        Node importedChapter = newDocument.importNode(chapter, true);
        bookElement.appendChild(importedChapter);

        // Save the document to a file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");

        DOMSource source = new DOMSource(newDocument);
        StreamResult result = new StreamResult(new File(UPLOAD_DIR + chapterTitle + "_translated.xml"));
        transformer.transform(source, result);
    }
}
