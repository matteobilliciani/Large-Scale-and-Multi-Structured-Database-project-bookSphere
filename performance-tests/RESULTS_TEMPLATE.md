# 📊 Performance Test Results - BookSphere

> Questo documento fornisce un template per documentare i risultati dei test di performance nel report del progetto.

## 🖥️ Test Environment Configuration

### Hardware
- **CPU**: [es. Intel Core i7-9700K @ 3.60GHz, 8 cores]
- **RAM**: [es. 16 GB DDR4]
- **Storage**: [es. SSD NVMe 512GB]
- **OS**: [es. Windows 11 Pro]

### Software
- **Java Version**: 21
- **Spring Boot**: 3.5.10
- **MongoDB**: Atlas M0 Free Tier / Local Cluster
- **Neo4j**: 5.x Community Edition
- **Gatling**: 3.11.5

### Database Configuration
- **MongoDB**:
  - Connection: [Atlas / Local]
  - Replica Set: [Yes/No, numero di nodi]
  - Dataset Size: [es. 100k books, 50k authors]

- **Neo4j**:
  - Connection: Local
  - Node Count: [numero di nodi]
  - Relationship Count: [numero di relazioni]

## 📈 Test Results Summary

### Test 1: Basic Load Test

**Configuration**:
- Concurrent Users: 50
- Ramp-up Time: 30 seconds
- Test Duration: 2 minutes
- Test Date: [inserire data]

**Results**:

| Metric | Value | Status |
|--------|-------|--------|
| Total Requests | [es. 12,450] | ✅ |
| Success Rate | [es. 99.2%] | ✅ |
| Failed Requests | [es. 95 (0.8%)] | ✅ |
| **Response Times** | | |
| Min | [es. 45ms] | ✅ |
| Mean | [es. 245ms] | ✅ |
| Max | [es. 2.1s] | ✅ |
| 50th Percentile (Median) | [es. 180ms] | ✅ |
| 75th Percentile | [es. 320ms] | ✅ |
| 95th Percentile | [es. 890ms] | ✅ |
| 99th Percentile | [es. 1.5s] | ✅ |
| **Throughput** | | |
| Requests/sec (mean) | [es. 142 req/s] | ✅ |
| Requests/sec (max) | [es. 185 req/s] | ✅ |

**Analysis**:
- [Inserire osservazioni: es. "Il sistema ha gestito bene il carico base con response time medi sotto il secondo"]
- [Endpoint più lenti: es. "L'endpoint /api/books/filter con sorting ha mostrato i tempi più alti (media 450ms)"]
- [Eventuali errori: es. "8 timeout dovuti a connessione MongoDB temporaneamente saturata"]

**Screenshot**: 
[Inserire screenshot del report Gatling: Response Time Distribution, Active Users Over Time]

---

### Test 2: Stress Test

**Configuration**:
- Load Pattern: 10 → 200 users (progressive)
- Increment: +10 users every 30 seconds
- Max Duration: 15 minutes
- Test Date: [inserire data]

**Results**:

| Metric | Value | Status |
|--------|-------|--------|
| Total Requests | [es. 45,230] | ✅ |
| Success Rate | [es. 92.5%] | ⚠️ |
| Failed Requests | [es. 3,392 (7.5%)] | ⚠️ |
| **Response Times** | | |
| Min | [es. 52ms] | ✅ |
| Mean | [es. 1.2s] | ⚠️ |
| Max | [es. 8.5s] | ⚠️ |
| 95th Percentile | [es. 4.5s] | ⚠️ |
| 99th Percentile | [es. 7.8s] | ⚠️ |
| **Throughput** | | |
| Requests/sec (mean) | [es. 315 req/s] | ✅ |
| Requests/sec (max) | [es. 425 req/s] | ✅ |

**Breaking Point Identified**: 
- [es. "Il sistema inizia a degradare oltre i 180 utenti concorrenti"]
- [es. "Error rate sale dal 2% a 15% tra 170 e 200 utenti"]

**Analysis**:
- [Performance sotto stress]
- [Bottleneck identificati]
- [Comportamento del sistema al limite]

**Screenshot**: 
[Response Time Percentiles Over Time, Requests Per Second]

---

### Test 3: Spike Test

**Configuration**:
- Normal Load: 10 users
- Spike Load: 200 users (instant)
- Spike Duration: 60 seconds
- Test Date: [inserire data]

**Results**:

| Metric | Value | Status |
|--------|-------|--------|
| Total Requests | [es. 18,750] | ✅ |
| Success Rate | [es. 87.3%] | ⚠️ |
| Failed Requests | [es. 2,381 (12.7%)] | ⚠️ |
| **Response Times During Spike** | | |
| Mean | [es. 2.8s] | ⚠️ |
| Max | [es. 9.2s] | ❌ |
| 95th Percentile | [es. 8.2s] | ⚠️ |
| 99th Percentile | [es. 9.0s] | ❌ |

**Resilience Analysis**:
- [Tempo di ripristino dopo il picco]
- [Comportamento durante il picco]
- [Raccomandazioni per gestire spike]

**Screenshot**: 
[Active Users Over Time mostrando lo spike, Response Time vs RPS]

---

### Test 4: Database Intensive Test

**Configuration**:
- Concurrent Users: 30
- Test Duration: 10 minutes
- Focus: MongoDB Aggregations + Neo4j Queries
- Test Date: [inserire data]

**Results**:

| Metric | MongoDB Operations | Neo4j Operations | Cross-DB Consistency |
|--------|-------------------|------------------|---------------------|
| Success Rate | [es. 97.2%] | [es. 95.8%] | [es. 96.1%] |
| Mean Response Time | [es. 1.5s] | [es. 2.1s] | [es. 1.8s] |
| 95th Percentile | [es. 3.2s] | [es. 4.8s] | [es. 3.8s] |

**Database Performance Analysis**:
- **MongoDB**:
  - [Aggregation pipeline performance]
  - [Index effectiveness]
  - [Connection pool usage]

- **Neo4j**:
  - [Graph query performance]
  - [Cypher optimization]
  - [Memory usage]

- **Consistency**:
  - [Sincronizzazione tra i due DB]
  - [Eventuali inconsistenze rilevate]

**Screenshot**: 
[Request Statistics per endpoint]

---

### Test 5: Endurance Test (Optional)

**Configuration**:
- Constant Load: 50 users
- Test Duration: 2 hours
- Test Date: [inserire data]

**Results**:

| Metric | Start (0-10 min) | Middle (55-65 min) | End (110-120 min) | Trend |
|--------|------------------|-------------------|-------------------|-------|
| Mean Response Time | [es. 280ms] | [es. 310ms] | [es. 325ms] | ↗️ Slight increase |
| 95th Percentile | [es. 920ms] | [es. 1.1s] | [es. 1.2s] | ↗️ Stable |
| Success Rate | [es. 99.8%] | [es. 99.7%] | [es. 99.5%] | ↘️ Very stable |
| Memory Usage (App) | [es. 1.2GB] | [es. 1.4GB] | [es. 1.5GB] | ↗️ Minor leak? |

**Long-term Stability Analysis**:
- [Degradazione performance nel tempo]
- [Memory leaks identificati]
- [Stabilità generale del sistema]

---

## 🎯 Overall Performance Assessment

### Strengths
1. ✅ [es. "Excellent performance under normal load (<1s response time)"]
2. ✅ [es. "Good scalability up to 150 concurrent users"]
3. ✅ [es. "MongoDB aggregations perform well with proper indexes"]
4. ✅ [es. "System recovers quickly after spike events"]

### Weaknesses
1. ⚠️ [es. "Performance degrades significantly above 180 users"]
2. ⚠️ [es. "Neo4j queries slower than expected for complex graphs"]
3. ⚠️ [es. "Connection pool exhaustion under spike load"]
4. ⚠️ [es. "Some endpoints show inconsistent response times"]

### Bottlenecks Identified
1. 🔴 **[es. Database Connection Pool]**
   - Issue: [descrizione]
   - Impact: [impatto sulle performance]
   - Recommendation: [soluzione proposta]

2. 🔴 **[es. Unoptimized Query on /api/books/filter]**
   - Issue: [descrizione]
   - Impact: [impatto]
   - Recommendation: [soluzione]

### Optimization Implemented
1. ✅ **[es. Added index on books.categories]**
   - Before: 850ms average
   - After: 210ms average
   - Improvement: 75% faster

2. ✅ **[es. Increased connection pool size]**
   - Before: 10 connections
   - After: 50 connections
   - Impact: Reduced errors from 8% to 2%

## 📋 Conclusions

### Performance Metrics Achievement

| Requirement | Target | Achieved | Status |
|-------------|--------|----------|--------|
| Avg Response Time (normal load) | < 1s | [es. 245ms] | ✅ |
| 95th Percentile (normal load) | < 2s | [es. 890ms] | ✅ |
| Success Rate | > 95% | [es. 99.2%] | ✅ |
| Concurrent Users (acceptable perf) | 100+ | [es. 150] | ✅ |
| System Breaking Point | 150+ | [es. 180] | ✅ |

### Recommendations for Production

1. **Scalability**:
   - [Suggerimenti per scaling orizzontale/verticale]
   - [Configurazione replica set]
   - [Sharding strategy]

2. **Performance**:
   - [Ottimizzazioni query]
   - [Caching strategy]
   - [CDN per static assets]

3. **Monitoring**:
   - [Metriche da monitorare]
   - [Alert thresholds]
   - [Logging strategy]

## 📚 Appendix

### Tools and Methodology
- Performance testing tool: Gatling 3.11.5
- Test methodology: Progressive load testing with realistic user scenarios
- Reporting: HTML interactive reports with detailed metrics

### Test Scenarios Coverage
- ✅ Public API access (unauthenticated)
- ✅ User authentication flow
- ✅ Authenticated user operations
- ✅ Database-intensive operations
- ✅ Cross-database consistency
- ✅ System resilience to spikes
- ✅ Long-term stability

### References
- [Link to Gatling reports]
- [Link to monitoring dashboards]
- [Link to test scripts repository]

---

**Test Conducted By**: [Nome Team]
**Test Period**: [Data inizio] - [Data fine]
**Report Version**: 1.0
**Last Updated**: [Data]
