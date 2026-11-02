package baseup.service;

import baseup.model.Identifiable;
import java.util.*;
import java.util.function.Predicate;

/** ArrayList 기반의 공통 객체 관리. 외부에는 수정 불가능한 복사본을 반환한다. */
public final class Manager<T extends Identifiable> {
    private final List<T> items = new ArrayList<>();
    public Manager(List<T> initial) { initial.forEach(this::add); }
    public List<T> all() { return List.copyOf(items); }
    public Optional<T> find(String id) { return items.stream().filter(x -> x.id().equals(id)).findFirst(); }
    public List<T> search(Predicate<T> match) { return items.stream().filter(match).toList(); }
    public void add(T item) {
        if (find(item.id()).isPresent()) throw new IllegalArgumentException("중복 ID: " + item.id());
        items.add(item);
    }
    public void remove(String id) { items.removeIf(x -> x.id().equals(id)); }
}
