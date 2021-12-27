import com.github.ubaifadhli.annotations.CSVColumn;
import lombok.ToString;

import java.util.List;

@ToString
public class DummyData {
    private String name;
    private int number;

    @CSVColumn(splitByCharacter = '|')
    private List<String> splitTexts;
}
