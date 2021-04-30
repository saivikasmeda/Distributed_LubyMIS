public class Message {

    enum Type {
        APPLICATION, RAND_VAL, I_AM_WINNER, I_AM_LOSER;
    }

    Node sender;
    Node receiver;
    String data;
    Message.Type type;

    public Message(Node sender, Node receiver, String data, Type type) {
        this.sender = sender;
        this.receiver = receiver;
        this.data = data;
        this.type = type;
    }

    public Message() {

    }
}
