package org.example;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.json.JSONObject;

public class TranslateXML {
    private static final int MAX_QUERY_LENGTH = 500;

    public static void convertToMultiLingualXML(String inputFile, String outputFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document englishDoc = builder.parse(new File(inputFile));
            englishDoc.getDocumentElement().normalize();

            Document multiLangDoc = builder.newDocument();

            Element langElement = multiLangDoc.createElement("Lang");
            multiLangDoc.appendChild(langElement);

            Element hiElement = multiLangDoc.createElement("hi");
            langElement.appendChild(hiElement);

            Element fontname = multiLangDoc.createElement("fontname");
            fontname.setTextContent("Mangal");
            hiElement.appendChild(fontname);

            Element book = multiLangDoc.createElement("book");
            hiElement.appendChild(book);

            NodeList chapterList = englishDoc.getElementsByTagName("chapter");

            for (int i = 0; i < chapterList.getLength(); i++) {
                Element chapter = (Element) chapterList.item(i);
                Element newChapter = multiLangDoc.createElement("chapter");

                // Handle chapter title
                String chapterTitle = chapter.getAttribute("title");
                appendTranslatedTag(multiLangDoc, newChapter, "title_en", chapterTitle);
                appendTranslatedTag(multiLangDoc, newChapter, "title", translateLongText(chapterTitle, "en", "hi"));

                NodeList sections = chapter.getElementsByTagName("section");

                for (int j = 0; j < sections.getLength(); j++) {
                    Element section = (Element) sections.item(j);
                    Element newSection = multiLangDoc.createElement("section");

                    // Handle section title
                    String sectionTitle = section.getAttribute("title");
                    appendTranslatedTag(multiLangDoc, newSection, "title_en", sectionTitle);
                    appendTranslatedTag(multiLangDoc, newSection, "title", translateLongText(sectionTitle, "en", "hi"));

                    NodeList children = section.getChildNodes();
                    for (int k = 0; k < children.getLength(); k++) {
                        Node child = children.item(k);
                        if (child.getNodeType() != Node.ELEMENT_NODE) continue;

                        Element elem = (Element) child;
                        if (elem.getTagName().equals("paragraph")) {
                            String originalText = elem.getTextContent().trim();

                            Element newParagraph = multiLangDoc.createElement("paragraph");

                            appendTranslatedTag(multiLangDoc, newParagraph, "text_en", originalText);
                            appendTranslatedTag(multiLangDoc, newParagraph, "text", translateLongText(originalText, "en", "hi"));

                            newSection.appendChild(newParagraph);
                        } else if (elem.getTagName().equals("image")) {
                            Node importedImage = multiLangDoc.importNode(elem, true);
                            newSection.appendChild(importedImage);
                        }
                    }
                    newChapter.appendChild(newSection);
                }

                book.appendChild(newChapter);
            }

            saveXML(multiLangDoc, outputFile);
            System.out.println("Translate.xml created successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void appendTranslatedTag(Document doc, Element parent, String tagName, String content) {
        Element element = doc.createElement(tagName);
        element.setTextContent(content);
        parent.appendChild(element);
    }

    public static String translateLongText(String text, String sourceLang, String targetLang) throws IOException {
        String processedText = preprocessTextForTranslation(text);

        List<String> chunks = splitText(processedText, MAX_QUERY_LENGTH);
        StringBuilder translatedText = new StringBuilder();

        for (String chunk : chunks) {
            translatedText.append(translateText(chunk, sourceLang, targetLang)).append(" ");
        }

        return translatedText.toString().trim();
    }

    public static List<String> splitText(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            chunks.add(text.substring(start, end));
            start = end;
        }

        return chunks;
    }

    public static String translateText(String text, String sourceLang, String targetLang) throws IOException {
        String urlStr = "https://api.mymemory.translated.net/get?q=" +
                URLEncoder.encode(text, "UTF-8") + "&langpair=" + sourceLang + "|" + targetLang;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getJSONObject("responseData").getString("translatedText");
        }
    }

    public static String preprocessTextForTranslation(String text) {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("XML", "Extensible Markup Language");
        replacements.put("CPU", "Central Processing Unit");
        replacements.put("RAM", "Random Access Memory");
        replacements.put("API", "Application Programming Interface");
        // Add more as needed

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    public static void saveXML(Document doc, String outputFile) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outputFile));
        transformer.transform(source, result);
    }
}
