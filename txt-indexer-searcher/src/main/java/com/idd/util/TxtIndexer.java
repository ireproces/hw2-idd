package com.idd.util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;


public class TxtIndexer {

    private final IndexWriter writer;

    public TxtIndexer(String indexPath) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexPath)); // Crea un oggetto Directory per l'indice
        Analyzer analyzer = new StandardAnalyzer(); // Crea un analizzatore per l'indice
        IndexWriterConfig config = new IndexWriterConfig(analyzer); // Assegna l'analizzatore alla configurazione dell'IndexWriter
        
        // Imposta l'IndexWriter in modalità CREATE per sovrascrivere l'indice esistente
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(dir, config);
    }

    public void indexTxtFiles(String txtFolderPath) throws IOException {
        File txtFolder = new File(txtFolderPath); // Crea un oggetto File per la cartella contenente i papers .txt

        if (!txtFolder.exists() || !txtFolder.isDirectory()) {
            System.out.println("La directory specificata non esiste: " + txtFolderPath);
            return;
        }

        File[] txtFiles = txtFolder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (txtFiles == null || txtFiles.length == 0) {
            System.out.println("Nessun file .txt trovato in " + txtFolderPath);
            return;
        }

        System.out.println("Found " + txtFiles.length + " .txt files in directory: " + txtFolderPath + "\n");
        
        System.out.println("Starting indexing...");
        long startTime = System.currentTimeMillis(); // Inizia il timer
        int count = 0; // Contatore per i file indicizzati
        for (File file : txtFiles) {
            if (file.canRead()) {
                System.out.println("Indexing file: " + file.getName());
                indexSingleFile(file); // Funzione per indicizzare un singolo file
                count++;
            } else {
                System.out.println("Cannot read file: " + file.getName());
            }
        }

        long endTime = System.currentTimeMillis();
        double totalSeconds = (endTime - startTime) / 1000.0;
        double avgSeconds = totalSeconds / Math.max(count, 1);

        System.out.printf("Indexing completed.");
        System.out.printf("\nStatistics:\n");
        System.out.printf("• Total files indexed: %d\n", count);
        System.out.printf("• Total time: %.2f seconds\n", totalSeconds);
        System.out.printf("• Average time per file: %.2f seconds\n", avgSeconds);

        writer.close();
        System.out.println("IndexWriter closed.");
    }

    private String extractTitle(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Title:")) {
                    return line.substring(6).trim();
                }   
            }
        }
        return "Unknown title";
    }

    private String extractAbstract(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean abstractFound = false;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Abstract:")) {
                    abstractFound = true;
                    sb.append(line.substring(9).trim()).append(" ");
                } else if (abstractFound) {
                    sb.append(line.trim()).append(" ");
                }
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : "No abstract available";
    }


    private void indexSingleFile(File file) throws IOException {
        String title = extractTitle(file);
        String abstractText = extractAbstract(file);

        Document doc = new Document(); // Crea un documento Lucene per l'indice

        // Aggiunge i campi del paper al documento Lucene con analizzatori appropriati
        doc.add(new StringField("path", file.getAbsolutePath(), Field.Store.YES));
        doc.add(new StringField("filename", file.getName(), Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("abstract", abstractText, Field.Store.YES));

        writer.addDocument(doc);

        System.out.println("Indexed file: " + file.getName());
    }

    public static void main(String[] args) {
        try {
            String indexPath = "indexes_lucene"; // Specifica la directory dove salvare gli indici
            String txtFolderPath = "downloadfiles/papers_txt"; // Specifica la directory dei file .txt
            TxtIndexer indexer = new TxtIndexer(indexPath);
            indexer.indexTxtFiles(txtFolderPath);

            try (DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)))) {
                System.out.println("Total documents in index: " + reader.numDocs());
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
