# BKV Inspector App

A BKV Inspector App egy Android alkalmazás, amely belső ellenőrzési jegyzőkönyvek gyors és hatékony rögzítésére, majd PDF formátumban történő generálására készült.  
Az alkalmazás bemutató verziója BKV villamos ágazatának csoportvezetők ellenőrzési tevékenységéhez lett alakítva, de más területek igényeihez is testre szabható.
Az alkalmazásban két ellenőrzési típus között enged választani, támogatja külső adatfájlok betöltését, valamint automatikusan formázott, kétoszlopos PDF jelentések készítését.

----------------------------------------------------------------------------------------------------------------------------------------------------

## Fő funkciók

- Járművezetői adatok betöltése (név + törzsszám)
- Ellenőri személyek betöltése (név + azonosítószám)
- Viszonylatok és helyszínek importálása
- Ellenőrzés típusának választása (Mentori / Vonali)
- Kezdő–végpont, időpontok, járműadatok rögzítése
- Pozitív és negatív észrevételek többválasztós listából
- Megjegyzések hozzáadása
- Automatikus PDF generálás fejléccel
- PDF megnyitása vagy tovább osztása FileProvideren keresztül

----------------------------------------------------------------------------------------------------------------------------------------------------

## TXT fájlok formátuma

Az alkalmazás három darab `.txt` adatfájlt használ, amelyek bármikor cserélhetők. Az alkalmazáson belül tallózhatóak.

### 1. locations.txt
Formátum:
1,Bécsi út / Vörösvári út
2B,Török Flóris utca
14,Szent István tér (Újpesti piac)

### 2. sampledrivers.txt 
Formátum:
Michael Schumacher,2134
Ayrton Senna,A2741
Niki Lauda,B7851

### 3. sampleinspectors.txt
Ivan the Terrible,99124
Vlad the Impaler,78014
Hammurabi,55904

----------------------------------------------------------------------------------------------------------------------------------------------------

A fájlok betallózása az alkalmazás Beállítások menüjében történik.

---

##  Használat:

1. Nyisd meg az alkalmazást.
2. A **Beállítások** menüben tallózd be az adatfájlokat:
   - járművezetők (`sampledrivers.txt`)
   - ellenőrök (`sampleinspectors.txt`)
   - viszonylatok és helyszínek (`locations.txt`)
3. A főoldalon add meg:
   - ellenőr nevét és azonosítóját
   - járművezető adatait
   - viszonylatot, járműszámot
   - kezdő–vég helyszínt és időpontokat
   - ellenőrzés típusát (Mentori / Vonali)
4. Válaszd ki a pozitív és negatív megállapításokat.
5. Írj megjegyzést, ha szükséges.
6. Kattints: **PDF generálás**

---

##  PDF felépítése

A PDF az alábbi információkat tartalmazza:

- Fejléc (png)
- Ellenőrzés helye és ideje
- Ellenőr neve és azonosító száma
- Ellenőrzés típusa (mentori / vonali)
- Járművezető neve, törzsszáma, viszonylat, pályaszám
- Kezdő és végpont idővel
- Pozitív és negatív észrevételek -két hasábban
- Megjegyzés szövegdoboz
- Automatikus lezárási időbélyeg

A PDF-ek a következő helyre kerülnek:
"Android/data/com.amerthamer.bkvinspector/files/Documents/Ellenőrzések/"

----------------------------------------------------------------------------------------------------------------------------------------------------

##  Fejlesztési adatok

- Nyelv: Kotlin  
- UI: Material Design 3, ViewBinding  
- PDF: Android PdfDocument API  
- Fájlkezelés: FileProvider + Storage Access Framework  
- Min SDK: 24  
- Package name: `com.amerthamer.bkvinspector`  

----------------------------------------------------------------------------------------------------------------------------------------------------

## Mintaadatok

A GitHub repó `samples/` mappájában találhatók a minta `.txt` fájlok:

- `sampledrivers.txt` — járművezető minta  
- `sampleinspectors.txt` — ellenőrzést végző személy minta  
- `locations.txt` — viszonylatok és helyszínek mintái



----------------------------------------------------------------------------------------------------------------------------------------------------

## Bővíthetőség

Az alkalmazás struktúrája rugalmas, így könnyen módosítható:

- más ellenőrzési igényekhez
- új ellenőrzés típusokra
- CSV / Excel importálásra
- backend API-ra
- vállalati logó / dizájn hozzáadására

----------------------------------------------------------------------------------------------------------------------------------------------------
