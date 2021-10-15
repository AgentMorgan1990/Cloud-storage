import java.io.IOException;

import com.geekbrains.Command;

public interface Callback {
    void call(Command cmd) throws IOException, InterruptedException;
}
