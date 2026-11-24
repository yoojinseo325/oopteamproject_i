package baseup.service;

import baseup.model.RuleConcept;
import baseup.repository.Catalog;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 오늘의 규칙(기능 2). 전체 규칙 중 하나를 날짜에 따라 무작위로 고른다.
 *
 * - 같은 날에는 프로그램을 다시 켜도 같은 규칙이 나온다(날짜로 정해지는 무작위).
 * - 한 해 동안 규칙을 섞은 순서대로 하루에 하나씩 보여 주므로, 전체 규칙 수(40일)만큼은 겹치지 않는다.
 * - 문제를 풀지 않은 규칙도 나온다. 문제와 무관하게 규칙을 소개하는 기능이기 때문이다.
 *
 * <pre>
 * DailyRuleService daily = new DailyRuleService(service.catalog(), Clock.systemDefaultZone());
 * RuleConcept rule = daily.today();
 * RuleConcept other = daily.another(rule, new Random()); // "다른 규칙 보기" 버튼
 * </pre>
 */
public final class DailyRuleService {
    private final List<RuleConcept> rules;
    private final Clock clock;

    public DailyRuleService(Catalog catalog, Clock clock) { this(catalog.rules(), clock); }

    public DailyRuleService(List<RuleConcept> rules, Clock clock) {
        if (rules.isEmpty()) throw new IllegalArgumentException("오늘의 규칙으로 보여 줄 규칙이 없습니다.");
        this.rules = List.copyOf(rules);
        this.clock = clock;
    }

    /** 오늘(clock 기준 날짜)의 규칙. */
    public RuleConcept today() { return forDate(LocalDate.now(clock)); }

    /** 특정 날짜의 규칙. 같은 날짜는 항상 같은 결과다. */
    public RuleConcept forDate(LocalDate date) {
        List<RuleConcept> order = yearlyOrder(date.getYear());
        return order.get((date.getDayOfYear() - 1) % order.size());
    }

    /** current를 뺀 나머지 중 무작위 하나. 규칙이 하나뿐이면 그 규칙을 돌려준다. */
    public RuleConcept another(RuleConcept current, Random random) {
        List<RuleConcept> others = rules.stream().filter(r -> current == null || !r.id().equals(current.id())).toList();
        return others.isEmpty() ? rules.get(0) : others.get(random.nextInt(others.size()));
    }

    /** 오늘부터 days일 동안의 규칙(예고 화면이나 테스트용). */
    public List<RuleConcept> upcoming(int days) {
        LocalDate start = LocalDate.now(clock);
        List<RuleConcept> result = new ArrayList<>();
        for (int i = 0; i < days; i++) result.add(forDate(start.plusDays(i)));
        return result;
    }

    /** 연도를 시드로 섞은 순서. java.util.Random은 시드가 같으면 어느 PC에서나 같은 순서를 만든다. */
    private List<RuleConcept> yearlyOrder(int year) {
        List<RuleConcept> order = new ArrayList<>(rules);
        Collections.shuffle(order, new Random(0xBA5E_0000L + year));
        return order;
    }
}
