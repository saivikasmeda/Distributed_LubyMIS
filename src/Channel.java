import java.util.concurrent.Callable;

public class Channel implements Callable<Boolean> {

    private static String TAG = "Channel";

    Message message;

    public Channel(Message message) {
        this.message = message;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            return message.receiver.receive(message);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
