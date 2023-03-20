package stirling.software.SPDF.controller;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import stirling.software.SPDF.utils.PdfUtils;
import stirling.software.SPDF.utils.ProcessExecutor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Controller
public class CompressController {

    private static final Logger logger = LoggerFactory.getLogger(CompressController.class);

    @GetMapping("/compress-pdf")
    public String compressPdfForm(Model model) {
        model.addAttribute("currentPage", "compress-pdf");
        return "compress-pdf";
    }

    
    @PostMapping("/compress-pdf")
    public ResponseEntity<byte[]> optimizePdf(
            @RequestParam("fileInput") MultipartFile inputFile,
            @RequestParam("optimizeLevel") int optimizeLevel,
            @RequestParam(name = "fastWebView", required = false) Boolean fastWebView,
            @RequestParam(name = "jbig2Lossy", required = false) Boolean jbig2Lossy) throws IOException, InterruptedException {

        // Save the uploaded file to a temporary location
        Path tempInputFile = Files.createTempFile("input_", ".pdf");
        inputFile.transferTo(tempInputFile.toFile());

        // Prepare the output file path
        Path tempOutputFile = Files.createTempFile("output_", ".pdf");

        // Prepare the OCRmyPDF command
        List<String> command = new ArrayList<>();
        command.add("ocrmypdf");
        command.add("--optimize");
        command.add(String.valueOf(optimizeLevel));

        if (fastWebView != null && fastWebView) {
            long fileSize = inputFile.getSize();
            long fastWebViewSize = (long) (fileSize * 1.25); // 25% higher than file size
            command.add("--fast-web-view");
            command.add(String.valueOf(fastWebViewSize));
        }

        if (jbig2Lossy != null && jbig2Lossy) {
            command.add("--jbig2-lossy");
        }

        command.add(tempInputFile.toString());
        command.add(tempOutputFile.toString());

        int returnCode = ProcessExecutor.runCommandWithOutputHandling(command);
        
        // Read the optimized PDF file
        byte[] pdfBytes = Files.readAllBytes(tempOutputFile);

        // Clean up the temporary files
        Files.delete(tempInputFile);
        Files.delete(tempOutputFile);

        // Return the optimized PDF as a response
        String outputFilename = inputFile.getOriginalFilename().replaceFirst("[.][^.]+$", "") + "_Optimized.pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", outputFilename);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
}

}