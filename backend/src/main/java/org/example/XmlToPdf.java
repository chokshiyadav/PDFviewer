package org.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.MimeConstants;

public class XmlToPdf {
    public static void convertXmlToPdf(String xmlFilePath, String xslFilePath, String pdfOutputPath) {
        try {
            // Initialize FOP factory
            FopFactory fopFactory = FopFactory.newInstance(new File("").toURI());

            // Create FOUserAgent instance
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

            // Output PDF file
            File pdfFile = new File(pdfOutputPath);
            OutputStream out = new FileOutputStream(pdfFile);

            try {
                // Create FOP instance for PDF conversion
                Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

                // Set up transformer for XSL-FO processing
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer transformer = factory.newTransformer(new StreamSource(new File(xslFilePath)));

                // Source XML file
                StreamSource src = new StreamSource(new File(xmlFilePath));

                // Perform transformation and generate PDF
                transformer.transform(src, new SAXResult(fop.getDefaultHandler()));

                System.out.println("PDF successfully created: " + pdfOutputPath);
            } finally {
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
