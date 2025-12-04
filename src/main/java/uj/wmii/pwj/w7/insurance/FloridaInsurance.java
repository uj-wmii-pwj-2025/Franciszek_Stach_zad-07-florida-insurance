package uj.wmii.pwj.w7.insurance;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.US;

public class FloridaInsurance {

    private static final String DATA_FILE_ZIP = "FL_insurance.csv.zip";
    private static final String DATA_FILE = "FL_insurance.csv";
    private static final String COUNT_FILE = "count.txt";
    private static final String TIV2012_FILE = "tiv2012.txt";
    private static final String MOST_VALUABLE_FILE = "most_valuable.txt";

    public static void main(String[] args) {
        List<List<String>> data = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(DATA_FILE_ZIP)) {

            ZipEntry entry = zipFile.stream()
                    .filter(e -> e.getName().equals(DATA_FILE))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Plik danych nie znaleziony w ZIP"));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry), UTF_8))) {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    data.add(Arrays.asList(line.split(",")));
                }
            }

            createCountFile(data);
            createTiv2012File(data);
            createMostValuableFile(data);

        } catch (Exception e) {
            System.err.println("Wystąpił błąd podczas przetwarzania danych: " + e.getMessage());
        }
    }

    private static void createCountFile(List<List<String>> data) throws IOException {
        Set<String> counties = data.stream()
                .map(row -> row.get(2).trim())
                .collect(Collectors.toSet());

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(COUNT_FILE))) {
            writer.write(String.valueOf(counties.size()));
        }
    }

    private static void createTiv2012File(List<List<String>> data) throws IOException {
        double totalTiv2012 = data.stream()
                .mapToDouble(row -> {
                    String tiv2012String = row.get(8).trim().replace(",", "");
                    return parseDouble(tiv2012String);
                })
                .sum();

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(TIV2012_FILE))) {
            writer.write(String.format(US, "%.2f", totalTiv2012));
        }
    }

    private static void createMostValuableFile(List<List<String>> data) throws IOException {
        Map<String, Double> growthMap = data.stream()
                .collect(Collectors.groupingBy(
                        row -> row.get(2).trim(),
                        Collectors.summingDouble(row -> {
                            double t2012 = parseDouble(row.get(8).trim().replace(",", ""));
                            double t2011 = parseDouble(row.get(7).trim().replace(",", ""));
                            return t2012 - t2011;
                        })
                ));

        List<Map.Entry<String, Double>> top10 = growthMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(MOST_VALUABLE_FILE))) {
            writer.write("country,value");
            writer.newLine();
            for (Map.Entry<String, Double> e : top10) {
                writer.write(e.getKey() + "," + String.format(US, "%.2f", e.getValue()));
                writer.newLine();
            }
        }
    }
}