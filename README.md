# Java-Certificate-Database

Simple database alike program for storing and managing rows with certificates informations.

### Notes

Program uses binary files for storing data. Indexes are build with use of serializable TreeMaps (self-balancing Red Black Trees). Each tree stores corresponding keys and lists of positions of records in "db.bin" main file.

---

### Technologies

- JavaSE-17

---

### Features

- [x] Expiring certificates warning (2 months period)
- [x] Auto update of certificate status to "expired"
- [x] Adding new rows
- [x] Listing all rows
- [x] Searching by attribute
- [x] Importing and exporting CSV file
- [x] Editing data

---

### Program preview

![Java certificate database program preview](/images/program-preview.png)
