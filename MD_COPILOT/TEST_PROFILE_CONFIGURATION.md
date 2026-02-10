# Configurazione Centralizzata del Profilo di Test

## 📋 Overview

Tutti i test nel progetto BookSphere ora utilizzano un'annotazione personalizzata `@TestProfile` per centralizzare la configurazione del profilo Spring attivo.

## 🎯 Come Funziona

### File Principale: `TestProfile.java`

L'annotazione `@TestProfile` è definita in:
```
src/test/java/it/unipi/bookSphere/TestProfile.java
```

### Cambiare il Profilo per TUTTI i Test

**Per cambiare il profilo utilizzato da TUTTI i test, modifica UNA SOLA RIGA:**

```java
@ActiveProfiles("clusterWSL")  // <-- CAMBIA SOLO QUESTA RIGA
```

Valori comuni:
- `"clusterWSL"` - Cluster MongoDB/Neo4j su WSL
- `"cluster"` - Cluster MongoDB/Neo4j
- `"local"` - Database locali
- `"wsl"` - Configurazione WSL

## 📝 Utilizzo nei Test

Tutti i test ora usano semplicemente:

```java
@SpringBootTest
@TestProfile  // <-- Invece di @ActiveProfiles("clusterWSL")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MyControllerTest {
    // test methods...
}
```

## ⚠️ Eccezione

**BookSphereApplicationTests** continua ad usare `@ActiveProfiles` direttamente perché richiede **due profili** contemporaneamente:

```java
@ActiveProfiles({"clusterWSL", "test-connection"})
```

## ✅ Vantaggi

1. **Un'unica modifica**: Cambi il profilo in un solo file invece di 25+ file
2. **Meno errori**: Impossibile dimenticare di aggiornare qualche file
3. **Più veloce**: Modifichi 1 riga invece di cercare in tutti i test
4. **Manutenibile**: Più facile gestire la configurazione nel tempo

## 🔍 File Aggiornati

- ✅ Tutti i test in `src/test/java/it/unipi/bookSphere/open/`
- ✅ Tutti i test in `src/test/java/it/unipi/bookSphere/registered/`
- ✅ Tutti i test in `src/test/java/it/unipi/bookSphere/admin/`
- ✅ Quasi tutti i test in `src/test/java/it/unipi/bookSphere/OLD/`
  - Eccezione: `BookSphereApplicationTests` (richiede 2 profili)
