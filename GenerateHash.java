import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GenerateHash <password>");
            System.exit(1);
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(args[0]);
        System.out.println(hash);
    }
}
