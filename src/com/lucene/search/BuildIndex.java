package com.lucene.search;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.UnicodeWhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.search.grouping.GroupingSearch;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.util.Arrays;



public class BuildIndex {

//////////////////////train/////////////////////////////////
//	private static String csvInputTrain = "source/train/train_cui_joint.tsv";
//	private static String csvInputUmls = "source/umls/umls_new_joint.tsv";
//	private static String csvInputUmlsPt = "source/umls_pt/umls_pt_joint.tsv";
//
//	private static String csvTarget = "data/train/input_1_cuiless_train.tsv";
//	private static String module = "TrainUmls";
//
//	private static String indexDirTrain = "index/train/"+ module +"/";
//	private static String indexDirUmls = "index/umls/"+ module +"/";
//	private static String indexDirUmlsPt = "index/umls/"+ module +"Pt/";
//	private static String csvOutput = "output/train/" + module +"PtUmls_train_40/";

//////////////////////train_split_dev /////////////////////////////////
//private static String csvInputTrain = "source/train_split/train_joint.tsv";
//private static String csvInputUmls = "source/umls/umls_new_joint.tsv";
//private static String csvInputUmlsPt = "source/umls_pt/umls_pt_joint.tsv";
//
//private static String csvTarget = "data/test_dev/input_1_cuiless_dev.tsv";
//private static String module = "TrainUmls";
//
//private static String indexDirTrain = "index/train_split/"+ module +"/";
//private static String indexDirUmls = "index/umls/"+ module +"/";
//private static String indexDirUmlsPt = "index/umls/"+ module +"Pt/";
//private static String csvOutput = "output/test_dev/" + module +"PtUmls_dev_exp1_40/";

    ////////////////////test /////////////////////////////////
    private static String sourceGeonames = "ontology/allCountries.txt";

    private static String sourceQueries = "mentions/allmentions_dev.csv";

    private static String indexGeonames = "index/geoname/";

    private static String searchOutput = "output/";

    public static void main(String[] args) throws Exception
    {
        File directory = new File(searchOutput);
        if(!directory.exists()){directory.mkdir();}

        BufferedReader br = null;
        String line = "";
        String txtSplitBy = "\t";
        String csvSplitBy = "\t";
//
//		IndexWriter writerGeonames = geonamesCreateWriter(indexGeonames);
//        br = new BufferedReader(new FileReader(sourceGeonames));
//        while ((line = br.readLine()) != null) {
//                // use comma as separator
//                String[] concepts = line.split(txtSplitBy);
//        		List<Document> documentlist = createDocumentGeonames(concepts[0], concepts[1], concepts[2], concepts[3], concepts[8], concepts[9], concepts[14]);
//                writerGeonames.addDocuments(documentlist);
//        		documentlist.clear();
//        }
//        writerGeonames.close();


//
        IndexSearcher searcherGeonames = createSearcher(indexGeonames);


        ArrayList<ArrayList<String>> listOMentions = new ArrayList<ArrayList<String>>();
        ArrayList<String> mentions = new ArrayList<String>();
        br = new BufferedReader(new FileReader(sourceQueries));
        FileWriter csvWriter = new FileWriter(searchOutput+ "mentions_dev_predictions_strategy10.csv");
        while ((line = br.readLine()) != null) {
            // System.out.println(line);
            // use comma as separator
            String[] rows = line.split(csvSplitBy);

            ArrayList<String[]> results = searchByMention(rows[0], searcherGeonames);
            csvWriter.append(rows[0]);
            csvWriter.append("\t");
            csvWriter.append(rows[1]);
            csvWriter.append("\t");
            for (String[] result:results) {
//                System.out.println(result[0]);
//                System.out.println(result[1]);
                csvWriter.append(result[0]);
                csvWriter.append(" ");
                csvWriter.append(result[1]);
                csvWriter.append(" ");
                csvWriter.append(result[2]);
                csvWriter.append(" ");
                csvWriter.append(result[3]);
                csvWriter.append("\t");
            }

            System.out.println("----------------------------------------------");
            csvWriter.append("\n");

        }
        csvWriter.flush();
        csvWriter.close();
    }


    private static List<Document> createDocumentGeonames(String ID, String canonicalName, String asciiName, String alternateNames, String countrycode, String alternatecc, String population)
    {
        List<Document> documents = new ArrayList<>();
        String[] alternateNameAray = alternateNames.split(",");
        String[] canonicalNameAray = {canonicalName,asciiName};
        List<String> stopWords = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by",
                "for", "if", "in", "into", "", "is", "it", "no", "not", "of",
                "on", "or", "such", "that", "the", "their", "then", "there", "these",
                "they", "this", "to", "was", "will", "with","he","she","patient",
                "&apos;d", "&apos;s", "&quot;", "&lt;", "&gt;","his","her");
        int a = alternateNameAray.length;
        int b = canonicalNameAray.length;
        alternateNameAray = Arrays.copyOf(alternateNameAray, a + b);// 数组扩容
        System.arraycopy(canonicalNameAray, 0, alternateNameAray, a, b);
        List<String> allofnames_temp = new ArrayList<String>();
        for(String name_temp: alternateNameAray){
            if(name_temp.length()!= 0){
                allofnames_temp.add(name_temp);
            }
        }
        String[] allofnames = allofnames_temp.toArray(new String[allofnames_temp.size()]);
        for (String name : allofnames) {
            Document document = new Document();
            document.add(new StoredField("idField", ID));
            document.add(new StoredField("canonicalNameField", canonicalName));
            document.add(new TextField("nameField", name , Field.Store.YES));
            document.add(new TextField("ngramsField", name , Field.Store.YES));
            document.add(new TextField("tokenField", name , Field.Store.YES));
            document.add(new TextField("countrycodeField", countrycode , Field.Store.YES));
            document.add(new TextField("alternateccField", alternatecc , Field.Store.YES));
            List<String> name_allWords = new ArrayList<>(Arrays.asList(name.split(" ")));
            name_allWords.removeAll(stopWords);
            List<String> first_character_name_allwords = new ArrayList<String>();
            //System.out.println(name);
            for (String temp : name_allWords){
//                System.out.println(temp);
//                System.out.println(temp.length());
//                System.out.println(name);
//                if(temp.length()==0){
//                    first_character_name_allwords.add("********");
//                }
//                else{
//                    first_character_name_allwords.add(temp.substring(0,1));
//                }
                first_character_name_allwords.add(temp.substring(0,1));
            }
            String abbreviationName = String.join("", first_character_name_allwords);
            document.add(new TextField("abbreviationField", abbreviationName , Field.Store.YES));
//            System.out.println(abbreviationName);
            document.add(new StoredField("populationField", Long.parseLong(population)));

            documents.add(document);
        }
        documents.get(allofnames.length-1).add(new StringField("IDEndField", "x" , Field.Store.NO));

        return documents;
    }


    private static IndexWriter geonamesCreateWriter(String INDEX_DIR) throws IOException
    {
        FSDirectory dir = FSDirectory.open(Paths.get(INDEX_DIR));
        ExactMatchAnalyzer Exactanalyzer = new ExactMatchAnalyzer();
        NGramAnalyzer Nganalyzer = new NGramAnalyzer();
        StandardEnglishAnalyzer tokenanlyzer = new StandardEnglishAnalyzer();
        AbbreviationAnalyzer abbreviationanalyzer = new AbbreviationAnalyzer();
        Map analyzerMapGeonames = new HashMap();
        analyzerMapGeonames.put("nameField", Exactanalyzer);
        analyzerMapGeonames.put("ngramsField", Nganalyzer);
        analyzerMapGeonames.put("tokenField", tokenanlyzer);
        analyzerMapGeonames.put("countrycodeField", Exactanalyzer);
        analyzerMapGeonames.put("alternateccField", Exactanalyzer);
        analyzerMapGeonames.put("abbreviationField", abbreviationanalyzer);
        PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer(),analyzerMapGeonames);
        IndexWriterConfig config = new IndexWriterConfig(perFieldAnalyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(dir, config);
        return writer;
    }

    private static ArrayList<String[]> searchByMention(String mention, IndexSearcher searcherGeonames) throws Exception
    {

        ExactMatchAnalyzer Exactanalyzer = new ExactMatchAnalyzer();
        NGramAnalyzer Nganalyzer = new NGramAnalyzer();
        StandardEnglishAnalyzer tokenanlyzer = new StandardEnglishAnalyzer();
        AbbreviationAnalyzer abbreviationanalyzer = new AbbreviationAnalyzer();
        Map analyzerMap = new HashMap();
        analyzerMap.put("nameField", Exactanalyzer);
        analyzerMap.put("ngramsField", Nganalyzer);
        analyzerMap.put("tokenField", tokenanlyzer);
        analyzerMap.put("countrycodeField", Exactanalyzer);
        analyzerMap.put("alternateccField", Exactanalyzer);
        analyzerMap.put("abbreviationField", abbreviationanalyzer);
        PerFieldAnalyzerWrapper perFieldAnalyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer(),analyzerMap);

        Query idEndQuery  = new TermQuery(new Term("IDEndField", "x"));
        GroupingSearch groupingSearch = new GroupingSearch(idEndQuery);
        QueryParser nameQueryParser = new QueryParser("nameField", perFieldAnalyzer);
        QueryParser NgQueryParser = new QueryParser("ngramsField", perFieldAnalyzer);
        QueryParser tokenQueryParser = new QueryParser("tokenField", perFieldAnalyzer);
        QueryParser coutrycodeQueryParser = new QueryParser("countrycodeField", perFieldAnalyzer);
        QueryParser alternateccQueryParser = new QueryParser("alternateccField", perFieldAnalyzer);
        QueryParser abbreviationQueryParser = new QueryParser("abbreviationField", perFieldAnalyzer);
        System.out.println(mention);
        String filterSpecialMention = mention.replaceAll("[\\pP\\p{Punct}]","");
        String filterSpecialSpaceMention = filterSpecialMention.replace(" ","");
//        String escapedQueryString = QueryParser.escape(mention);
        System.out.println(filterSpecialSpaceMention);
        ArrayList<String[]> results;
//	    String luceneSpecialCharacters = """([-+&|!(){}\[\]^"~*?:\\/\s])""";
//	    val escapedQueryString = mention.replaceAll(luceneSpecialCharacters, """\\$1""")

        // first look for an exact match of the input phrase (the "name" field ignores spaces, punctuation, etc.)


        results =  scoredEntries(groupingSearch,  searcherGeonames,  nameQueryParser,  filterSpecialSpaceMention,3000,"Exact_Match");
//        if (results.isEmpty()) {
//            results.addAll(scoredEntries(groupingSearch,  searcherGeonames,  tokenQueryParser,  filterSpecialMention,1200,"Token_Match"));
//        }
//
//        if (results.isEmpty()) {
//            results = scoredEntries(groupingSearch,   searcherGeonames,  nameQueryParser,  filterSpecialSpaceMention +"~",5,"Fuzzy_Match" );
//        }
//        if (results.isEmpty()) {
//                results = scoredEntries(groupingSearch,  searcherGeonames,  NgQueryParser,  filterSpecialMention,60,"Ngram_Match");
//            }
//        if (results.isEmpty()) {
//            results.addAll(scoredEntries(groupingSearch,  searcherGeonames,  abbreviationQueryParser,  filterSpecialSpaceMention,1000,"Abbreviation_Match"));
//        }

        results.addAll(scoredEntries(groupingSearch,   searcherGeonames,  nameQueryParser,  filterSpecialSpaceMention +"~",5,"Fuzzy_Match" ));
        results.addAll(scoredEntries(groupingSearch,  searcherGeonames,  NgQueryParser,  filterSpecialMention,60,"Ngram_Match"));
        results.addAll(scoredEntries(groupingSearch,  searcherGeonames,  tokenQueryParser,  filterSpecialMention,1200,"Token_Match"));

        results.addAll(scoredEntries(groupingSearch,  searcherGeonames,  abbreviationQueryParser,  filterSpecialSpaceMention,5000,"Abbreviation_Match"));
        results.addAll(scoredEntries(groupingSearch,  searcherGeonames,  coutrycodeQueryParser,  filterSpecialSpaceMention,5000,"Countrycode_Match"));
//        results.addAll(scoredEntries(groupingSearch,  searcherGeonames,  alternateccQueryParser,  filterSpecialSpaceMention,5000,"Alternatecc_Match"));


//	    else if (!results.isEmpty() && results.size()<20){
//	    	System.out.println("%%%%%%%%%%%%%%");
//	    	System.out.println(results.size());
//	    	results.addAll(scoredEntries(groupingSearch,  searcher,  NgQueryParser,  escapedQueryString));
//	    	System.out.println(results.size());
//
//	    }


        return results;
    }

    private static IndexSearcher createSearcher(String indx_dir) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indx_dir));
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        return searcher;
    }

    private static ArrayList<String[]> scoredEntries(GroupingSearch groupingSearch, IndexSearcher searcher, QueryParser queryparser
            , String querystring, int maxHits, String type) throws IOException, ParseException {
        TopGroups<?> topGroups =  groupingSearch.search(searcher, queryparser.parse(querystring), 0, maxHits);

        ArrayList<String[]> listOLists = new ArrayList<String[]>();
        if (topGroups == null) return listOLists;
        else {
            for (GroupDocs groupdocs: topGroups.groups){
                Document headDoc = searcher.doc(groupdocs.scoreDocs[0].doc);
                String[] Info = new String[] {headDoc.get("idField"),String.valueOf(headDoc.get("populationField")),type,Float.toString(groupdocs.maxScore)};
                listOLists.add(Info);
            }
            return listOLists;
        }
    }

}


