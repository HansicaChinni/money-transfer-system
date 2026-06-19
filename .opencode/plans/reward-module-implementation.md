# Reward Module — Complete Implementation Plan

## Overview
Add a reward points system where users earn 1 point per ₹100 actually debited (floor, >100 threshold) on successful outgoing transfers, and can redeem points 1:1 for rupees during future transfers.

## Architecture

```
User initiates transfer (with useRewardPoints flag)
  │
  ▼
TransferServiceImpl.doTransferOnce()
  ├── If useRewardPoints: calculate rewardUsed = min(user.rewardPoints, amount)
  │   ├── actualDebit = amount - rewardUsed
  │   ├── from.debit(actualDebit)
  │   ├── to.credit(amount)
  │   ├── from.debitRewardPoints(rewardUsed)
  │   └── save RewardTransaction(REDEEMED, rewardUsed)
  │
  ├── Else: existing debit/credit logic (actualDebit = amount)
  │
  ├── accountRepo.save(from)  ← persists balance + rewardPoints atomically
  ├── accountRepo.save(to)
  ├── logWriter.logSuccess(...)
  │
  └── After success: rewardGrantWriter.grantPoints(fromId, txId, actualDebit)
        └── if actualDebit > 100 → points = floor(actualDebit / 100)
              └── RewardTransaction(EARNED, points) + from.creditRewardPoints(points)
```

---

## Backend — Files to Create (12)

### 1. `domain/enums/RewardTransactionType.java`
```java
package com.money.draft.domain.enums;
public enum RewardTransactionType { EARNED, REDEEMED }
```

### 2. `domain/entity/RewardTransaction.java`
- Immutable entity with: id, accountId (Long), type (ENUM), points (int), referenceTransactionId (Long, nullable), createdOn (Instant)
- Static factories: `earned(accountId, points, referenceTxId)`, `redeemed(accountId, points, referenceTxId)`
- @PrePersist sets createdOn
- Indexes on accountId and referenceTransactionId

### 3. `domain/repository/RewardTransactionRepository.java`
- `findByAccountIdOrderByCreatedOnDesc(Long)`
- `findByAccountIdAndTypeOrderByCreatedOnDesc(Long, RewardTransactionType)`
- `findByAccountIdAndReferenceTransactionIdAndType(Long, Long, RewardTransactionType)`
- `@Query sumPointsByAccountIdAndType(Long accountId, RewardTransactionType type)`

### 4. `dto/RewardTransactionResponse.java`
```java
public record RewardTransactionResponse(
    Long id, Long accountId, String type, int points,
    Long referenceTransactionId, LocalDateTime createdOn
) {}
```

### 5. `dto/RewardSummaryResponse.java`
```java
public record RewardSummaryResponse(int currentPoints, int totalEarned, int totalRedeemed) {}
```

### 6. `dto/TransactionDetailResponse.java`
```java
public record TransactionDetailResponse(
    Long id, Long fromAccountId, Long toAccountId,
    BigDecimal amount, String status, String failureReason,
    String idempotencyKey, LocalDateTime createdOn,
    Integer rewardPointsEarned, Integer rewardPointsUsed
) {}
```

### 7. `exception/InsufficientRewardPointsException.java`
- extends BusinessException("INSUFFICIENT_REWARD_POINTS", message)

### 8. `service/RewardService.java` — interface
```java
void grantPoints(Long accountId, Long transactionId, BigDecimal actualDebit);
RewardSummaryResponse getRewardSummary(Long accountId);
List<RewardTransactionResponse> getRewardTransactions(Long accountId);
TransactionDetailResponse getTransactionDetail(Long accountId, Long transactionId);
```

### 9. `service/RewardGrantWriter.java` — REQUIRES_NEW helper
```java
@Service
public class RewardGrantWriter {
    private final AccountRepository accountRepo;
    private final RewardTransactionRepository rewardRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void grantPoints(Long accountId, Long transactionId, BigDecimal actualDebit) {
        if (actualDebit == null || actualDebit.compareTo(new BigDecimal("100")) <= 0) return;
        int points = actualDebit.divide(new BigDecimal("100"), RoundingMode.DOWN).intValue();
        if (points <= 0) return;
        Account account = accountRepo.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        account.creditRewardPoints(points);
        accountRepo.save(account);
        rewardRepo.save(RewardTransaction.earned(accountId, points, transactionId));
    }
}
```

### 10. `service/impl/RewardServiceImpl.java`
```java
@Service
public class RewardServiceImpl implements RewardService {
    // Dependencies: AccountRepository, RewardTransactionRepository

    public int redeemDuringTransfer(Account from, BigDecimal amount) {
        int available = from.getRewardPoints();
        if (available <= 0) return 0;
        int amountInt = amount.setScale(0, RoundingMode.DOWN).intValue();
        int used = Math.min(available, amountInt);
        from.debitRewardPoints(used);
        return used;
    }

    public RewardSummaryResponse getRewardSummary(Long accountId) {
        Account account = accountRepo.findById(accountId).orElseThrow(...);
        int earned = rewardRepo.sumPointsByAccountIdAndType(accountId, EARNED);
        int redeemed = rewardRepo.sumPointsByAccountIdAndType(accountId, REDEEMED);
        return new RewardSummaryResponse(account.getRewardPoints(), earned, redeemed);
    }

    public TransactionDetailResponse getTransactionDetail(Long accountId, Long transactionId) {
        TransactionLog tx = txRepo.findById(transactionId).orElseThrow(...);
        // fetch reward earned record for this account+tx
        Optional<RewardTransaction> earned = rewardRepo.findByAccountIdAndReferenceTransactionIdAndType(accountId, transactionId, EARNED);
        Optional<RewardTransaction> redeemed = rewardRepo.findByAccountIdAndReferenceTransactionIdAndType(accountId, transactionId, REDEEMED);
        return new TransactionDetailResponse(
            tx.getId(), tx.getFromAccountId(), tx.getToAccountId(),
            tx.getAmount(), tx.getStatus().name(), tx.getFailureReason(),
            tx.getIdempotencyKey(), toLocalDateTime(tx.getCreatedOn()),
            earned.map(RewardTransaction::getPoints).orElse(null),
            redeemed.map(RewardTransaction::getPoints).orElse(null)
        );
    }
}
```

### 11. `controller/RewardController.java` — user endpoints
```
GET  /me/rewards/summary       → RewardSummaryResponse
GET  /me/rewards/transactions  → List<RewardTransactionResponse>
```

### 12. `controller/AdminRewardController.java` — admin endpoints
```
GET  /admin/rewards            → List<RewardTransactionResponse>
GET  /admin/rewards/{id}       → RewardSummaryResponse for account
```

---

## Backend — Files to Modify (7)

### 1. `domain/entity/Account.java`
- Add field: `@Column(nullable = false) private int rewardPoints;`
- Add methods:
```java
public void creditRewardPoints(int points) {
    requireActive();
    if (points <= 0) throw new ValidationException("points must be positive");
    this.rewardPoints += points;
    touch();
}
public void debitRewardPoints(int points) {
    requireActive();
    if (points <= 0) throw new ValidationException("points must be positive");
    if (this.rewardPoints < points) throw new InsufficientRewardPointsException(this.id, this.rewardPoints, points);
    this.rewardPoints -= points;
    touch();
}
```
- Add getter/setter for `rewardPoints`

### 2. `dto/MeTransferRequest.java`
- Add field: `@Column(nullable = false) boolean useRewardPoints` (default false)

### 3. `dto/TransferRequest.java`
- Add field: `boolean useRewardPoints` (default false)

### 4. `dto/TransferResponse.java`
- Add fields: `Integer rewardPointsUsed`, `Integer rewardPointsEarned`

### 5. `dto/AccountResponse.java`
- Add field: `int rewardPoints`

### 6. `service/impl/TransferServiceImpl.java`
Key changes in `doTransferOnce()`:
```java
@Transactional
protected TransferResponse doTransferOnce(TransferRequest req) {
    Account from = accountRepo.findById(...);
    Account to = accountRepo.findById(...);
    // ... validation ...

    BigDecimal amount = req.amount();
    BigDecimal actualDebit = amount;
    int rewardPointsUsed = 0;

    if (req.useRewardPoints() && from.getRewardPoints() > 0) {
        int available = from.getRewardPoints();
        int amountInt = amount.setScale(0, RoundingMode.DOWN).intValue();
        rewardPointsUsed = Math.min(available, amountInt);
        actualDebit = amount.subtract(BigDecimal.valueOf(rewardPointsUsed));
        from.debitRewardPoints(rewardPointsUsed);
        rewardRepo.save(RewardTransaction.redeemed(from.getId(), rewardPointsUsed, null));
    }

    from.debit(actualDebit);
    to.credit(amount);
    accountRepo.save(from);
    accountRepo.save(to);

    TransactionLog tx = logWriter.logSuccess(from.getId(), to.getId(), amount, req.idempotencyKey());

    // Grant reward points on actualDe bit (fire-and-forget, REQUIRES_NEW)
    int pointsEarned = 0;
    try {
        BigDecimal grantBase = req.useRewardPoints() ? actualDebit : amount;
        if (grantBase.compareTo(new BigDecimal("100")) > 0) {
            pointsEarned = grantBase.divide(new BigDecimal("100"), RoundingMode.DOWN).intValue();
            rewardGrantWriter.grantPoints(from.getId(), tx.getId(), grantBase);
        }
    } catch (Exception ignored) {}

    return TransferResponse.success(tx.getId(), amount, rewardPointsUsed, pointsEarned);
}
```

- Add `RewardGrantWriter` and `RewardTransactionRepository` as constructor dependencies

### 7. `controller/UserController.java`
- Add endpoint:
```java
@GetMapping("/transactions/{id}")
public ResponseEntity<TransactionDetailResponse> transactionDetail(Authentication auth, @PathVariable Long id) {
    var user = userRepo.findByUsername(auth.getName()).orElseThrow();
    return ResponseEntity.ok(rewardService.getTransactionDetail(user.getAccountId(), id));
}
```
- Add `RewardService` as constructor dependency

---

## Frontend — Files to Create (4)

### 1. `core/services/reward.service.ts`
```typescript
@Injectable({ providedIn: 'root' })
export class RewardService {
  private apiUrl = 'http://localhost:8080/me/rewards';
  constructor(private http: HttpClient) {}
  getRewardSummary(): Observable<RewardSummaryResponse> { ... }
  getRewardTransactions(): Observable<RewardTransactionResponse[]> { ... }
}
```

### 2. `features/user/rewards/rewards.component.ts`
- Loads reward summary + reward transactions on init
- Handles modal open/close for transaction detail
- Fetches transaction detail via `GET /me/transactions/{id}`

### 3. `features/user/rewards/rewards.component.html`
```
<app-navbar>
<div class="container-custom fade-in">
  <!-- Reward Summary Card -->
  <div class="stat-card">
    <h2>{{ summary.currentPoints }}</h2>
    <p>Available Reward Points</p>
    <small>Total Earned: {{ summary.totalEarned }} | Redeemed: {{ summary.totalRedeemed }}</small>
  </div>

  <!-- Reward History Table -->
  <div class="card">
    <table class="table table-hover">
      <thead>
        <tr>
          <th>Date</th>
          <th>Type</th>
          <th>Points</th>
          <th>Reference Transaction</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let rw of rewardTransactions">
          <td>{{ rw.createdOn | date:'short' }}</td>
          <td><span class="badge" [class.badge-success]="rw.type==='EARNED'" [class.badge-warning]="rw.type==='REDEEMED'">{{ rw.type }}</span></td>
          <td>{{ rw.type === 'EARNED' ? '+' : '-' }}{{ rw.points }}</td>
          <td>
            <a *ngIf="rw.referenceTransactionId" (click)="openTxDetail(rw.referenceTransactionId)" class="tx-link">#{{ rw.referenceTransactionId }}</a>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- Transaction Detail Modal -->
  <div class="modal" *ngIf="showTxModal">
    <!-- shows txDetail data: amount, from/to, status, rewardPointsEarned, rewardPointsUsed -->
  </div>
</div>
```

### 4. `features/user/rewards/rewards.component.scss`
- Style the modal, links, etc.

---

## Frontend — Files to Modify (6)

### 1. `core/models/api.models.ts`
Add interfaces:
```typescript
export interface RewardTransactionResponse {
  id: number; accountId: number; type: string;
  points: number; referenceTransactionId: number | null; createdOn: string;
}
export interface RewardSummaryResponse {
  currentPoints: number; totalEarned: number; totalRedeemed: number;
}
export interface TransactionDetailResponse {
  id: number; fromAccountId: number; toAccountId: number;
  amount: number; status: string; failureReason: string | null;
  idempotencyKey: string; createdOn: string;
  rewardPointsEarned: number | null; rewardPointsUsed: number | null;
}
```
Update existing:
- `MeTransferRequest` — add `useRewardPoints: boolean`
- `TransferResponse` — add `rewardPointsUsed: number | null`, `rewardPointsEarned: number | null`
- `AccountResponse` — add `rewardPoints: number`

### 2. `core/services/user.service.ts`
- Add method: `getTransactionDetail(id: number): Observable<TransactionDetailResponse>`

### 3. `features/user/transfer/transfer.component.ts`
- Load reward summary on init alongside account info
- Add `useRewardPoints` to the form
- Compute and display estimated cash deduction when checkbox is toggled
- Pass `useRewardPoints` with form submission
- Display reward points used/earned in success message

### 4. `features/user/transfer/transfer.component.html`
- Add checkbox + info display after amount field:
```html
<div *ngIf="rewardSummary">
  <div class="form-check">
    <input type="checkbox" formControlName="useRewardPoints" (change)="updateRewardEstimate()">
    <label>Use reward points (Available: {{ rewardSummary.currentPoints }} pts = ₹{{ rewardSummary.currentPoints }})</label>
  </div>
  <div *ngIf="transferForm.get('useRewardPoints')?.value && rewardEstimate"> 
    <p>Points used: {{ rewardEstimate.used }} | Cash debit: ₹{{ rewardEstimate.cashDebit }}</p>
  </div>
</div>
```

### 5. `features/user/dashboard/dashboard.component.html`
- Add third stat card between balance and holder cards:
```html
<div class="col-md-4 mb-3">
  <div class="stat-card stat-card-green">
    <p>Reward Points</p>
    <h2>{{ accountInfo.rewardPoints }}</h2>
    <a routerLink="/user/rewards" class="btn btn-sm btn-outline-primary">View Rewards</a>
  </div>
</div>
```

### 6. `shared/components/navbar/navbar.component.html`
- Add under user links: `<li class="nav-item"><a class="nav-link" routerLink="/user/rewards" routerLinkActive="active">Rewards</a></li>`

### 7. `app.routes.ts`
- Add under user children:
```typescript
{
  path: 'rewards',
  loadComponent: () => import('./features/user/rewards/rewards.component').then(m => m.RewardsComponent)
}
```

---

## Key Reward Logic Summary

| Scenario | Transfer Amt | Reward Points | Actual Debit | Points Earned |
|----------|-------------|---------------|-------------|---------------|
| No rewards used | ₹500 | 0 | ₹500 | 5 |
| Use all 200 pts on ₹500 | ₹500 | 200 | ₹300 | 3 |
| Use all 600 pts on ₹500 | ₹500 | 500 | ₹0 | 0 (< threshold) |
| No rewards, ₹99 | ₹99 | 0 | ₹99 | 0 (< threshold) |

---

## Verification Checklist
- [ ] Build backend: `mvn compile` (no errors)
- [ ] Run backend: starts on port 8080
- [ ] Endpoint `POST /me/transfer` with `useRewardPoints: true` functions correctly
- [ ] Endpoint `GET /me/rewards/summary` returns correct totals
- [ ] Endpoint `GET /me/transactions/{id}` returns reward info
- [ ] Frontend compiles: `ng build`
- [ ] Dashboard shows reward points
- [ ] Transfer form shows reward checkbox with live estimate
- [ ] Rewards page shows history with clickable transaction IDs
- [ ] Modal opens with correct transaction + reward details
