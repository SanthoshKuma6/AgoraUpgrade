package in.sk.agora.agora.media;

/**
 * Created by santhosh on 20/12/2023.
 */
public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
