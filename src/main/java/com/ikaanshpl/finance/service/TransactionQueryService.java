package com.ikaanshpl.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ikaanshpl.finance.controller.ApiException;
import com.ikaanshpl.finance.dto.Dtos.MonthlySummary;
import com.ikaanshpl.finance.dto.Dtos.PageResponse;
import com.ikaanshpl.finance.dto.Dtos.TransactionFilter;
import com.ikaanshpl.finance.dto.Dtos.TransactionResponse;
import com.ikaanshpl.finance.entity.Transaction;
import com.ikaanshpl.finance.entity.TxType;
import com.ikaanshpl.finance.repo.TransactionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
@Transactional(readOnly = true)
public class TransactionQueryService {
	private static final Set<String> SORTABLE = Set.of("txDate", "amount", "createdAt", "id");

    private final TransactionRepository transactions;
    private final EntityManager em;
    

    public TransactionQueryService(TransactionRepository transactions, EntityManager em) {
		super();
		this.transactions = transactions;
		this.em = em;
	}

	public PageResponse<TransactionResponse> history(Long userId, TransactionFilter f, Pageable pageable) {
        validate(f);
        pageable.getSort().forEach(o -> {
            if (!SORTABLE.contains(o.getProperty())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Cannot sort by '" + o.getProperty() + "'. Allowed: " + SORTABLE);
            }
        });
        Page<TransactionResponse> page = transactions.findAll(spec(userId, f), pageable)
                .map(t -> TransactionResponse.from(t, null));
        return PageResponse.of(page);
    }

    public List<MonthlySummary> monthlySummary(Long userId, TransactionFilter f) {
        validate(f);
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Transaction> t = q.from(Transaction.class);

        Expression<BigDecimal> amount = t.get("amount");
        Expression<BigDecimal> income = cb.sum(cb.<BigDecimal>selectCase()
                .when(cb.equal(t.get("type"), TxType.INCOME), amount).otherwise(BigDecimal.ZERO));
        Expression<BigDecimal> expense = cb.sum(cb.<BigDecimal>selectCase()
                .when(cb.equal(t.get("type"), TxType.EXPENSE), amount).otherwise(BigDecimal.ZERO));

        q.multiselect(t.get("ym").alias("month"), income.alias("income"), expense.alias("expense"), cb.count(t).alias("cnt"));
        q.where(spec(userId, f).toPredicate(t, q, cb));
        q.groupBy(t.get("ym"));
        q.orderBy(cb.asc(t.get("ym")));

        return em.createQuery(q).getResultList().stream().map(r -> {
            BigDecimal inc = r.get("income", BigDecimal.class);
            BigDecimal exp = r.get("expense", BigDecimal.class);
            return new MonthlySummary(r.get("month", String.class), inc, exp, inc.subtract(exp), r.get("cnt", Long.class));
        }).toList();
    }

    private static void validate(TransactionFilter f) {
        if (f.from() != null && f.to() != null && f.from().isAfter(f.to())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'from' must not be after 'to'");
        }
    }

    /** Builds the WHERE clause. Predicates are only added for supplied filters (no "param IS NULL" tricks). */
    static Specification<Transaction> spec(Long userId, TransactionFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("userId"), userId));
            if (f.accountId() != null) p.add(cb.equal(root.get("account").get("id"), f.accountId()));
            if (f.categoryId() != null) p.add(cb.equal(root.get("category").get("id"), f.categoryId()));
            if (f.type() != null) p.add(cb.equal(root.get("type"), f.type()));
            if (f.from() != null) p.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("txDate"), f.from()));
            if (f.to() != null) p.add(cb.lessThanOrEqualTo(root.<LocalDate>get("txDate"), f.to()));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
