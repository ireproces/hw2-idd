package com.idd.util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class TxtSearcher {

    private IndexSearcher searcher;
    private QueryParser titleParser;
    private QueryParser abstractParser;
    // private long totalDocs;

    public TxtSearcher(String indexPath) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexPath));
        IndexReader reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
        // totalDocs = reader.numDocs();

        // Analyzer personalizzato per il titolo
        Analyzer titleAnalyzer = CustomAnalyzer.builder()
                .withTokenizer(WhitespaceTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                .build();

        // Analyzer standard per l'abstract
        Analyzer abstractAnalyzer = new StandardAnalyzer();

        titleParser = new QueryParser("title", titleAnalyzer);
        abstractParser = new QueryParser("abstract", abstractAnalyzer);
    }

    public void search(String queryStr) throws Exception {
        long startTime = System.currentTimeMillis(); // Inizia il timer

        BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
        String[] terms = queryStr.split(",");

        boolean titleSearched = false, abstractSearched = false;

        for (String term : terms) {
            String[] parts = term.split(":", 2);
            if (parts.length == 2) {
                String field = parts[0].trim();
                String fieldQuery = parts[1].trim();

                Query query = null;
                switch (field) {
                    case "title":
                        query = titleParser.parse(fieldQuery);
                        titleSearched = true;
                        break;
                    case "abstract":
                        query = abstractParser.parse(fieldQuery);
                        abstractSearched = true;
                        break;
                    default:
                        System.out.println("\nInvalid field. Valid fields are: title and abstract.\n");
                        return;
                }

                booleanQueryBuilder.add(query, BooleanClause.Occur.MUST);
            } else {
                System.out.println("\nInvalid query format. Use 'title:term', 'abstract:\"phrase\"' or 'title:x,abstract:y'.\n");
                return;
            }
        }

        BooleanQuery finalQuery = booleanQueryBuilder.build();
        TopDocs results = searcher.search(finalQuery, 10);
        long endTime = System.currentTimeMillis();

        long totalDocumentsReturned = results.totalHits.value();

        if (results.totalHits.value() > 0) {
            System.out.println("\n-----Documents-----");
            System.out.println("Found " + results.totalHits.value() + " documents.\n");

            for (ScoreDoc hit : results.scoreDocs) {
                Document doc = searcher.getIndexReader().storedFields().document(hit.doc);

                // Stampa solo i campi richiesti
                System.out.println("Title: " + (doc.get("title") != null ? doc.get("title") : "N/A"));
                System.out.println("Abstract: " + (doc.get("abstract") != null ? doc.get("abstract") : "N/A"));
                System.out.println("Score: " + hit.score);
                System.out.println("\n");
            }
        } else {
            System.out.println("\nNo documents found for the query.\n");
        }

        // Copertura dei campi interrogati
        System.out.println("-----Fields queried-----");
        System.out.printf("• Title queried: %b\n", titleSearched);
        System.out.printf("• Abstract queried: %b\n", abstractSearched);

        // Statistiche di ricerca
        System.out.println("\n-----Search Statistics-----");
        System.out.printf("• Total documents returned: %d\n", totalDocumentsReturned);
        // System.out.printf("• Total documents in the index: %d\n", totalDocs);

        double totalSeconds = (endTime - startTime) / 1000.0;
        System.out.printf("• Total time: %.2f seconds\n", totalSeconds);
        System.out.println("\n");
    }

    public void close() throws IOException {
        searcher.getIndexReader().close();
    }

    public static void main(String[] args) {
        try {
            String indexPath = "indexes_lucene";
            TxtSearcher searcher = new TxtSearcher(indexPath);

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("\nStarting...");
            System.out.println("Enter your query ('title:term', 'abstract:\"phrase\"', 'title:x,abstract:y').");
            System.out.println("Or type 'exit' to stop the search.\n");

            String line;
            while (true) {
                System.out.print("Query > ");

                line = br.readLine();
                if (line == null || line.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting...\n");
                    break;
                }

                if (!line.trim().isEmpty()) searcher.search(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
