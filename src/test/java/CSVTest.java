import com.github.ubaifadhli.core.CSVProcessor;

public class CSVTest {
    public void run() {
        CSVProcessor<DummyData> csvProcessor = new CSVProcessor<>("src/test/resources/test.csv", DummyData.class);

        csvProcessor.readFile().forEach(System.out::println);
    }
}
