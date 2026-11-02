package baseup.repository;

import java.io.IOException;
import java.util.List;

/** 저장 매체가 달라져도 서비스가 사용하는 메서드는 동일하다. */
public interface Repository<T> {
    List<T> load() throws IOException;
    void save(List<T> items) throws IOException;
}
