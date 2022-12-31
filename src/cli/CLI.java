package cli;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.stream.Stream;

import backend.DatabaseManagement;
import backend.NotFoundInDatabaseException;

import java.util.Random;

public class CLI {
	private Scanner scanner;
	private DatabaseManagement dbm;
	
	private String menu, 
			idString,
			name,
			surname,
			date,
			certificateSerialNumber,
			contractNumber,
			email,
			cardNumber,
			cardType,
			status,
			telephone,
			note;
	
	public CLI(){
		scanner = new Scanner(System.in);
		dbm = new DatabaseManagement("./db/db.bin");
		System.out.println("=== Certificate database ===");
		System.out.println("Created by Dominik Jalowiecki\n");
		statusUpdate();
		expiringRecords();
		menu();
	}
	
	private void header() {
		System.out.printf("%-4s|%-30s |%-35s |%-31s |%-16s |%-12s |%-8s |%-92s |%-18s |%-40s |%-17s |%-3s \n","ID","Name", "Surname", "Certificate expiration date", "Card number", "Card type", "Contract number", "Certificate serial number", "Telephone", "Email", "Status", "Note");
	}

	private void menu() {
		while(true) {
			System.out.println("-------------Menu-------------");
			System.out.println("1. Input data");
			System.out.println("2. Read all records");
			System.out.println("3. Search by attribute");
			System.out.println("4. Import/export CSV");
			System.out.println("5. Edit record");
			System.out.println("6. Exit program");
			System.out.print("> ");
			
			menu = scanner.nextLine().trim();
			System.out.println();
			while(!menu.matches("[1-6]")){
				System.out.println("Invalid response...");
				System.out.print("> ");
				menu = scanner.nextLine().trim();
			}
			
			switch(menu) {
				case "1": 
					inputData();
					break;
				case "2":
					readAll();
					break;
				case "3":
					searchByAttribute();
					break;
				case "4":
					importExportCSV();
					break;
				case "5":
					recordEdit();
					break;
				case "6":
					System.out.println("Exiting program...");
					System.exit(0);
			}
		}
	}
	
	private void readAll() {
		header();
		if(dbm.readRecords(this)) {
			System.out.println("ERROR! An error has occured during program execution...");
		}
		
		System.out.println();
	}
	
	private void inputData() {		
		name = name();
		surname = surname();
		date = date();
		cardNumber = cardNumber();
		cardType = cardType();
		contractNumber = contractNumber();
		telephone = telephone();
		email = email();
		status = status();
		certificateSerialNumber = certificateSerialNumber();
		note = note();
		
		System.out.println("Please wait, data input proceeds...\n");
		if(
			!dbm.insertData(
					name, 
					surname,
					date,
					Long.parseLong(cardNumber),
					Byte.parseByte(cardType), // 1-physical, 2-virtual, 3-other
					Integer.parseInt(contractNumber),
					certificateSerialNumber,
					Integer.parseInt(telephone),
					email,
					Byte.parseByte(status), // 1-new, 2-proforma sent, 3-paid, 4-expired, 5-renewed, 6-other
					note
			)
		) {
			System.out.println("New record added successfully!\n");
		} else {
			System.out.println("ERROR! An error has occured during program execution...\n");
		}
	}
	
	private<T> void searchByAttributeHelper(byte attribute, T value) {
		header();
		try {
			if(
				!dbm.readRecord(
						this,
						attribute,
						value
				)
			) {
				System.out.println();
			} else {
				System.out.println("ERROR! An error has occured during program execution...\n");
			}
		} catch(NotFoundInDatabaseException e) {
			System.out.println("ERROR! Records with entered attribute not found...\n");
		}
	}
	
	private void searchByAttribute() {
		byte attributeByte = 1; // Number of column/attribute in db file
		
		attributeByte = Byte.parseByte(menuSearch());
		
		switch(attributeByte) {
			case 1:
				idString = id();
				searchByAttributeHelper(attributeByte, Integer.parseInt(idString));
				break;
			case 2: 
				name = name();
				searchByAttributeHelper(attributeByte, name);
				break;
			case 3: 
				surname = surname();
				searchByAttributeHelper(attributeByte, surname);
				break;
			case 4: 
				date = date();
				searchByAttributeHelper(attributeByte, date);
				break;
			case 5: 
				cardNumber = cardNumber();
				searchByAttributeHelper(attributeByte, Long.parseLong(cardNumber));
				break;
			case 6:
				cardType = cardType();
				searchByAttributeHelper(attributeByte, Byte.parseByte(cardType));
				break;
			case 7: 
				contractNumber = contractNumber();
				searchByAttributeHelper(attributeByte, Integer.parseInt(contractNumber));
				break;
			case 8: 
				certificateSerialNumber = getCertificateSerialNumber();
				searchByAttributeHelper(attributeByte, certificateSerialNumber);
				break;
			case 9: 
				telephone = telephone();
				searchByAttributeHelper(attributeByte, Integer.parseInt(telephone));
				break;
			case 10: 
				email = email();
				searchByAttributeHelper(attributeByte, email);
				break;
			case 11: 
				status = status();
				searchByAttributeHelper(attributeByte, Byte.parseByte(status));
				break;
			case 12: 
				break;
		}
		
		System.out.println();
	}
	
	private String menuSearch() {
		System.out.println("----Search by attribute----");
		System.out.println("1. ID");
		System.out.println("2. Name");
		System.out.println("3. Surname");
		System.out.println("4. Certificate expiration time");
		System.out.println("5. Card number");
		System.out.println("6. Card type");
		System.out.println("7. Contract number");
		System.out.println("8. Certificate serial number");
		System.out.println("9. Telephone");
		System.out.println("10.Email");
		System.out.println("11.Certificate status");
		System.out.println("12.Return");
		System.out.print("> ");
		
		String response = scanner.nextLine().trim();

		while(!response.matches("([1-9]|1(0|1|2))")) {
			System.out.println("Invalid response...");
			System.out.print("> ");
			response = scanner.nextLine().trim();
		}
		
		return response;
	}
	
	private void importExportCSV() {
		String response = menuCSV();
		
		switch(response) {
			case "1": 
				importCSV();
				break;
			case "2":
				exportCSV();
				break;
			case "3":
				break;
		}
	}
	
	private String menuCSV() {
		System.out.println("----CSV----");
		System.out.println("1. Import CSV");
		System.out.println("2. Export CSV");
		System.out.println("3. Return");
		System.out.print("> ");
		
		String response = scanner.nextLine().trim();
		
		while(!response.matches("[1-3]")) {
			System.out.println("Invalid response...");
			System.out.print("> ");
			response = scanner.nextLine().trim();
		}
		
		return response;
	}
	
	private void importCSV() {
		System.out.println("Available files: ");
		Stream.of(new File("./").listFiles())
	      .filter(file -> !file.isDirectory())
	      .map(File::getName)
	      .filter(fileName -> fileName.matches("^.+\\.csv$"))
	      .forEach(el -> System.out.println("- " + el));
		
		String path = "";
		System.out.print("Enter file path to import data: ");
		
		path = scanner.nextLine().trim();
		if(!(new File(path).isFile())) {
			System.out.println("ERROR! File does not exist...\n");
			return;
		}
		
		if(!dbm.importCSV(this, path)) {
			System.out.println("CSV import successful!\n");
		} else {
			System.out.println("ERROR! An error has occured during program execution...\n");
		}
	}
	
	private void exportCSV() {
		String path = "";
		System.out.print("Enter file path to export data: ");
		
		path = scanner.nextLine().trim();
		if(new File(path).isFile()) {
			System.out.println("ERROR! File already exists...\n");
			return;
		}
				
		if(!dbm.exportCSV(path)) {
			System.out.println("CSV export successful!\n");
		} else {
			System.out.println("ERROR! An error has occured during program execution...\n");
		}
	}
	
	private void recordEdit() {
		int id = 0;
		
		byte statusByte = 0;
		System.out.println("---Edit menu---");
		System.out.println("1. Edit");
		System.out.println("2. Return");
		System.out.print("> ");
		
		menu = scanner.nextLine().trim();
		
		while(!menu.matches("[1-2]")){
			System.out.println("Invalid response...");
			System.out.print("> ");
			menu = scanner.nextLine().trim();
		}
		
		switch(menu) {
			case "1":
				idString = id();
				id = Integer.parseInt(idString);
				date = date();
				status = status();
				statusByte = Byte.parseByte(status);
				break;
			case "2":
				return;
		}
		
		try {
			if(!dbm.editRecord(id, date, statusByte)) {	
				System.out.println("Record edit successful!\n");
			} else {
				System.out.println("ERROR! An error has occured during program execution...\n");
			}
		} catch(NotFoundInDatabaseException e) {
			System.out.println("Entered ID not found...\n");
		}
	}
	
	private void expiringRecords() {
		System.out.println("Expiring certificates warning!");
		header();
		
		if(dbm.readExpiringRecords(this)) {
			System.out.println("ERROR! An error has occured during program execution...\n");
		}
		System.out.println();
	}
	
	public void printCSVImportError(String error) {
		System.out.println(error);
	}
	
	public void printRecord(
			int id,
			String name,
			String surname,
			String certificateExpirationDate,
			long cardNumber,
			byte cardType,
			int contractNumber,
			String certificateSerialNumber,
			int telephone,
			String email,
			byte status,
			String note
	) {
		String statusString;
		String cardTypeString;

		switch(cardType) {
			case 1:
				cardTypeString = "physical";
				break;
			case 2:
				cardTypeString = "virtual";
				break;
			default:
				cardTypeString = "other";
				break;
		}

		switch(status) {
			case 1:
				statusString = "new";
				break;
			case 2:
				statusString = "proforma sent";
				break;
			case 3:
				statusString = "paid";
				break;
			case 4:
				statusString = "expired";
				break;
			case 5:
				statusString = "renewed";
				break;
			default:
				statusString = "other";
				break;
		}

		System.out.printf("%-3d |%-30s |%-35s |%-31s |%-16d |%-12s |%-15d |===6FC0938=%-64s=96E94AF34432===  |+48 %-14d |%-40s |%-17s |%-3s \n",id,name,surname,certificateExpirationDate,cardNumber,cardTypeString,contractNumber,certificateSerialNumber,telephone,email,statusString,note);
	}
	
	private String name() {
		String name;
		
		System.out.print("Enter name: ");
		
		name = scanner.nextLine().trim();
		int len = name.length();
		
		while(!(len > 2 && len < 30)) {
			System.out.println("Entered invalid name (from 2 to 30 characters)...");
			System.out.print("Enter name: ");
			name = scanner.nextLine().trim();
			len = name.length();
		}
		
		return name;
	}
	
	private String surname() {
		String surname;
		
		System.out.print("Enter surname: ");
		
		surname = scanner.nextLine().trim();
		int len = surname.length();
		
		while(!(len > 2 && len < 35)) {
			System.out.println("Entered invalid surname (from 2 to 30 characters)...");
			System.out.print("Enter surname: ");
			surname = scanner.nextLine().trim();
			len = surname.length();
		}
		
		return surname;
	}
	
	private String date() {
		String date;
		
		System.out.print("Enter certificate expiration date and time (e.g. 31.05.2025 15:00:00): ");
		
		date = scanner.nextLine().trim();
		
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		formatter.setLenient(false);
		formatter.setTimeZone(TimeZone.getTimeZone("Europe/Warsaw"));
		
		while(true) {
			try {
				formatter.parse(date);
				break;
			} catch (ParseException | NumberFormatException e){
				System.out.println("Entered invalid date...");
				System.out.print("Enter certificate expiration date and time (e.g. 31.05.2025 15:00:00): ");
				date = scanner.nextLine().trim();
			}
		}
		
		return date;
	}
	
	private String cardNumber() {
		String cardNumber;
		
		System.out.print("Enter card number: ");
		
		cardNumber = scanner.nextLine().trim();
		
		while(!cardNumber.matches("^\\d{16}$")) {
			System.out.println("Entered invalid card number (16 digits)...");
			System.out.print("Enter card number: ");
			cardNumber = scanner.nextLine().trim();
		}
		
		return cardNumber;
	}
	
	private String cardType() {
		String cardType;
		
		System.out.println("---Card types---");
		System.out.println("1. Physical");
		System.out.println("2. Virtual");
		System.out.println("3. Other");
		System.out.print("Choose card type: ");
		
		cardType = scanner.nextLine().trim();
		
		while(!cardType.matches("[1-3]")) {
			System.out.println("Invalid response...");
			System.out.print("Choose card type: "); 
			cardType = scanner.nextLine().trim();
		}
		
		return cardType;
	}
	
	private String contractNumber() {
		String contractNumber;
		
		System.out.print("Enter contract number: ");
		
		contractNumber = scanner.nextLine().trim();
		
		while(!contractNumber.matches("^[0-9]{1,7}$")) {
			System.out.println("Invalid contract number (from 1 to 7 digits)...");
			System.out.print("Enter contract number: ");
			contractNumber = scanner.nextLine().trim();
		}
		
		return contractNumber;
	}
	
	private String telephone() {
		String telephone;
		
		System.out.print("Enter contact telephone number: ");
		
		telephone = scanner.nextLine().trim();
		
		while(!telephone.matches("^\\d{9}")) {
			System.out.println("Invalid telephone number (9 digits)...");
			System.out.print("Enter contact telephone number: ");
			telephone = scanner.nextLine().trim();
		}
		
		return telephone;
	}
	
	private String email() {
		String email;
		
		System.out.print("Enter email: ");
		
		email = scanner.nextLine().trim();
		
		while(!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
			System.out.println("Entered invalid email...");
			System.out.print("Enter email: ");
			email = scanner.nextLine().trim();
		}
		
		return email;
	}
	
	private String status() {
		String status;
		
		System.out.println("---Status---");
		System.out.println("1. New ");
		System.out.println("2. Proforma sent");
		System.out.println("3. Paid");
		System.out.println("4. Expired");
		System.out.println("5. Renewed");
		System.out.println("6. Other");
		System.out.print("Choose status: ");
		
		status = scanner.nextLine().trim();
		
		while(!status.matches("[1-6]")) {
			System.out.println("Invalid response...");
			System.out.print("Choose status: ");
			status = scanner.nextLine().trim();
		}
		
		return status;
	}
	
	private String certificateSerialNumber() {
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		
		for(int i = 0; i < 64; i++)
			sb.append((char) random.nextInt(97, 123));

		String randomString = sb.toString();		
		return randomString;
	}
	
	private String getCertificateSerialNumber() {
		String certificateSerialNumber;
		
		System.out.print("Enter certificate serial number: ");
		
		certificateSerialNumber = scanner.nextLine().trim();
		
		while(!(certificateSerialNumber.length() == 64)) {
			System.out.println("Entered invalid certificate serial number (64 characters, middle section)...");
			System.out.print("Enter certificate serial number: ");
			certificateSerialNumber = scanner.nextLine().trim();
		}
		
		return certificateSerialNumber;
	}
	
	private String note() {
		String note;
		
		System.out.print("Enter note: ");
		note = scanner.nextLine().trim();
		
		return note;
	}
	
	private String id() {
		String id;
		boolean error;
		
		do {
			error = false;
			
			System.out.print("Enter ID: ");
			id = scanner.nextLine().trim();
			
			try {
				Integer.parseInt(id);
			} catch(NumberFormatException e) {
				System.out.println("Entered invalid ID (only positive numbers)...");
				error = true;
			}
		} while(!id.matches("\\d+") || error);
		
		return id;
	}
	
	private void statusUpdate() {
		if(dbm.updateStatus()) {
			System.out.println("ERROR! An error has occured during program execution...\n");
		}
	}
}
