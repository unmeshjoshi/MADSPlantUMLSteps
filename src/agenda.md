# 2-Day Distributed Systems Workshop Agenda

> **Workshop Format**: Each teaching block is 40 minutes (≈ 30 min explanation + 10 min guided coding)  
> **Breaks**: 10–15 minutes between sessions  
> **Daily Structure**: ~4 hours teaching + ~45 minutes of breaks per day

---

## 📅 Day 1: Foundations & Basic Patterns

### **Session 1** (40 min) 🎯 **Why Distribute?**
- **Learning Goals:**
  - Resource ceilings and physical limits
  - Little's Law and performance modeling
  - Motivation for distributed patterns
- **🛠️ Hands-on Lab:** Run provided disk-perf test; capture own numbers
- **Break:** 10 minutes

### **Session 2** (40 min) 🎯 **Partial Failure Mindset**
- **Learning Goals:**
  - Probability of failure at scale
  - Network partitions and split-brain scenarios
  - Process pauses and their impact
- **🛠️ Hands-on Lab:** Walkthrough of the 'replicate' framework with an example test to inject faults.
- **Break:** 10 minutes

### **Session 3** (40 min) 🎯 **Write-Ahead Log Pattern**
- **Learning Goals:**
  - Append-only discipline for durability
  - Recovery mechanisms and replay
  - WAL as foundation for other patterns
- **🛠️ Hands-on Lab:** Execute and walkthrough WALTest for persistent key-value store.
- **Break:** 15 minutes

### **Session 4** (40 min) 🎯 **Replication & Majority Quorum**
- **Learning Goals:**
  - Write vs read quorums trade-offs
  - Quorum intersection properties
  - Universal Scalability Law curve analysis
- **🛠️ Hands-on Lab:** Modify `QuorumKVStoreTest`: pass for 5-node/3-node clusters
- **End of Day 1**

### 🍽️ **Lunch Break / Self-Paced Time**
**Offline Activities:**
- Review morning labs and concepts
- Push completed work to GitHub
- Optional: Explore additional resources

---

## 📅 Day 2: Consensus Algorithms & Advanced Patterns

### **Session 5** (40 min) 🎯 **Why Simple Replication Fails**
- **Learning Goals:**
  - Two-phase commit pitfalls
  - Recovery ambiguity problems
  - The need for consensus algorithms
- **🛠️ Hands-on Lab:** Step through `FailingTwoPhaseTest`; explain why it hangs
- **Break:** 10 minutes

### **Session 6** (40 min) 🎯 **Single-Value Paxos**
- **Learning Goals:**
  - Prepare/Accept phases explained
  - Recovery with generation numbers
  - Safety and liveness properties
- **🛠️ Hands-on Lab:** Implement `GenerationClockTest`: guarantee monotonicity
- **Break:** 10 minutes

### **Session 7** (40 min) 🎯 **From Paxos to Multi-Paxos**
- **Learning Goals:**
  - Replicated log concept and implementation
  - High-water mark for safe execution
  - Heartbeats and failure detection
- **🛠️ Hands-on Lab:** Extend log to multi-slot; make `HighWaterMarkTest` pass
- **Break:** 15 minutes

### **Session 8** (40 min) 🎯 **RAFT vs Multi-Paxos in Practice**
- **Learning Goals:**
  - Implementation optimizations comparison
  - Idempotent receiver pattern
  - Production considerations and future directions
- **🛠️ Hands-on Lab:** Compare RAFT & Multi-Paxos traces; annotate pros/cons
- **End of Day 2**

---

## 📊 Workshop Summary

### 🎯 **Learning Outcomes**
- **8 teaching blocks** × 40 minutes each
- **Hands-on labs** tied directly to core lecture concepts  
- **Built-in breaks** for focus & recovery
- **Progressive assignments** that reinforce distributed systems primitives step-by-step

### 🛠️ **Technical Skills Gained**
- Understanding distributed systems fundamentals
- Implementing Write-Ahead Log pattern
- Working with quorum-based replication
- Exploring consensus algorithms (Paxos, RAFT)
- Hands-on experience with fault tolerance patterns

### 📚 **Resources & Next Steps**
- All code examples and labs available on GitHub
- Additional reading materials provided
- Follow-up Q&A session for complex topics
