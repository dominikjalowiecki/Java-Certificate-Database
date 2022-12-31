package backend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.print.attribute.standard.DateTimeAtCompleted;

import cli.CLI;

public class DatabaseManagement {
	private String databaseFilePath = "";

	private int id = 0;
	private String name = "";
	private String surname = "";
	private String certificateExpirationDate = "";
	private long cardNumber = 0;
	private byte cardType = 0;
	private int contractNumber = 0;
	private String certificateSerialNumber = "";
	private int telephone = 0;
	private String email = "";
	private byte status = 0;
	private String note = "";
	
	private long dataPositionInFile = 0;
	
	public DatabaseManagement(String databaseFilePath) {
		this.databaseFilePath = databaseFilePath;
	}
	
	public boolean insertData(
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
		this.name = name;
		this.surname = surname;
		this.certificateExpirationDate = certificateExpirationDate;
		this.cardNumber = cardNumber;
		this.cardType = cardType;
		this.contractNumber = contractNumber;
		this.certificateSerialNumber = certificateSerialNumber;
		this.telephone = telephone;
		this.email = email;
		this.status = status;
		this.note = note;
		try(RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "rw")) {
			if(databaseFile.length() == 0) {
				this.id = 1;
				databaseFile.writeInt(this.id);
				databaseFile.writeUTF(this.name);
				databaseFile.writeUTF(this.surname);
				databaseFile.writeUTF(this.certificateExpirationDate);
				databaseFile.writeLong(this.cardNumber);
				databaseFile.writeByte(this.cardType);
				databaseFile.writeInt(this.contractNumber);
				databaseFile.writeUTF(this.certificateSerialNumber);
				databaseFile.writeInt(this.telephone);
				databaseFile.writeUTF(this.email);
				databaseFile.writeByte(this.status);
				databaseFile.writeUTF(this.note);
				databaseFile.writeInt(1); // number of records
			} else {
				long appendingStartPos = databaseFile.length() - 4;
				
				databaseFile.seek(appendingStartPos);
				int noRecords = databaseFile.readInt();
				this.id = noRecords + 1;
				int newNoRecords = this.id;
				databaseFile.seek(appendingStartPos);
				
				this.dataPositionInFile = appendingStartPos;
				
				databaseFile.writeInt(this.id);
				databaseFile.writeUTF(this.name);
				databaseFile.writeUTF(this.surname);
				databaseFile.writeUTF(this.certificateExpirationDate);
				databaseFile.writeLong(this.cardNumber);
				databaseFile.writeByte(this.cardType);
				databaseFile.writeInt(this.contractNumber);
				databaseFile.writeUTF(this.certificateSerialNumber);
				databaseFile.writeInt(this.telephone);
				databaseFile.writeUTF(this.email);
				databaseFile.writeByte(this.status);
				databaseFile.writeUTF(this.note);
				databaseFile.writeInt(newNoRecords);	
			}
			if(updateIndexes()) return true;
		} catch(FileNotFoundException e) {
			return true;
		} catch(IOException e) {
			return true;
		}
		
		return false;
	}
	
	public boolean editRecord(int id, String certificateExpirationDate, byte status) throws NotFoundInDatabaseException {
		this.id = id;
		this.certificateExpirationDate = certificateExpirationDate;
		this.status = status;
		
		String oldCertificateExpirationDate = "";
		byte oldStatus = 0;
		
		long tmpPos = 0;
		
		try(RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "rw")) {
			this.dataPositionInFile = getPositionFromIndex("./db/indexes/id.ser", this.id).get(0);

			databaseFile.seek(this.dataPositionInFile);
			databaseFile.readInt();
			databaseFile.readUTF();
			databaseFile.readUTF();
			
			tmpPos = databaseFile.getFilePointer();
			oldCertificateExpirationDate = databaseFile.readUTF();
			databaseFile.seek(tmpPos);
			databaseFile.writeUTF(this.certificateExpirationDate);
			
			databaseFile.readLong();
			databaseFile.readByte();
			databaseFile.readInt();
			databaseFile.readUTF();
			databaseFile.readInt();
			databaseFile.readUTF();
			
			tmpPos = databaseFile.getFilePointer();
			oldStatus = databaseFile.readByte();
			databaseFile.seek(tmpPos);
			databaseFile.writeByte(this.status);
			
			// Update certificate expiration date index
			SortedMap<Date, List<Long>> index = null;
			try (ObjectInputStream dataPositionInFileInput = new ObjectInputStream(new FileInputStream("./db/indexes/certificateExpirationDate.ser"))){
				index = (SortedMap<Date, List<Long>>) dataPositionInFileInput.readObject();			
			}
			
			SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			formatter.setTimeZone(TimeZone.getTimeZone("Europe/Warsaw"));
			Date oldDate = null;
			Date newDate = null;
			try {
				oldDate = formatter.parse((String) oldCertificateExpirationDate);
				newDate = formatter.parse(this.certificateExpirationDate);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			index.get(oldDate).remove(this.dataPositionInFile);
			
			List<Long> positions = null;
			if((positions = index.get(newDate)) != null) {
				positions.add(this.dataPositionInFile);
			} else {
				positions = new ArrayList<Long>();
				positions.add(this.dataPositionInFile);
				index.put(newDate, positions);
			}
			
			try (ObjectOutputStream dataPositionInFileOutput = new ObjectOutputStream(new FileOutputStream("./db/indexes/certificateExpirationDate.ser"))){
				dataPositionInFileOutput.writeObject(index);
			}
			
			// Update certificate status index
			SortedMap<Byte, List<Long>> index2 = null;
			try (ObjectInputStream dataPositionInFileInput = new ObjectInputStream(new FileInputStream("./db/indexes/status.ser"))){
				index2 = (SortedMap<Byte, List<Long>>) dataPositionInFileInput.readObject();			
			}
			
			index2.get(oldStatus).remove(this.dataPositionInFile);
			
			if((positions = index2.get(this.status)) != null) {
				positions.add(this.dataPositionInFile);
			} else {
				positions = new ArrayList<Long>();
				positions.add(this.dataPositionInFile);
				index2.put(this.status, positions);
			}
			
			try (ObjectOutputStream dataPositionInFileOutput = new ObjectOutputStream(new FileOutputStream("./db/indexes/status.ser"))){
				dataPositionInFileOutput.writeObject(index2);
			}
		} catch(ClassNotFoundException e) {
		} catch(NullPointerException e) {
			throw new NotFoundInDatabaseException();
		} catch(FileNotFoundException e) {
			return true;
		} catch(IOException e) {
			return true;
		}
		
		return false;
	}
	
	public boolean updateStatus() {
		try(RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "rw")) {
			SortedMap<Byte, List<Long>> dataPositionInFileStatus = null;
			SortedMap<Date, List<Long>> dataPositionInFileDate = null;
			try (
					ObjectInputStream dataPositionInFileInputStatus = new ObjectInputStream(new FileInputStream("./db/indexes/status.ser"));
					ObjectInputStream dataPositionInFileInputData = new ObjectInputStream(new FileInputStream("./db/indexes/certificateExpirationDate.ser"))
			) {
				dataPositionInFileStatus = (SortedMap<Byte, List<Long>>) dataPositionInFileInputStatus.readObject();
				TimeZone tz = TimeZone.getTimeZone("Europe/Warsaw");
				Calendar c = Calendar.getInstance(tz);
				Date data = new Date();
		        c.setTime(data);
				dataPositionInFileDate = ((SortedMap<Date, List<Long>>) dataPositionInFileInputData.readObject()).headMap(c.getTime());
			}
			
			List<Long> positionsPaid;
			if((positionsPaid = dataPositionInFileStatus.get((byte) 3)) == null)
				positionsPaid = new ArrayList<Long>();
			
			List<Long> positionsRenewed;
			if((positionsRenewed = dataPositionInFileStatus.get((byte) 5)) == null)
				positionsRenewed = new ArrayList<Long>();
			
			List<Long> positionsExpired;
			if((positionsExpired = dataPositionInFileStatus.get((byte) 4)) == null) {
				positionsExpired = new ArrayList<Long>();
				dataPositionInFileStatus.put((byte) 4, positionsExpired);
			}
			
			List<Long> positionsStatus = new ArrayList<Long>(positionsPaid);
			positionsStatus.addAll(positionsRenewed);
			List<Long> positionsData = new ArrayList<Long>();
			Collection<List<Long>> positionsTmp = dataPositionInFileDate.values();
			
			for(List<Long> list: positionsTmp) {
				for(Long position: list) {
					positionsData.add(position);
				}
			}
			
			positionsData.retainAll(positionsStatus);
			
			for(Long position: positionsData) {
				positionsPaid.remove(position);
				positionsRenewed.remove(position);
				positionsExpired.add(position);
								
				databaseFile.seek(position);
				databaseFile.readInt();
				databaseFile.readUTF();
				databaseFile.readUTF();
				databaseFile.readUTF();
				databaseFile.readLong();
				databaseFile.readByte();
				databaseFile.readInt();
				databaseFile.readUTF();
				databaseFile.readInt();
				databaseFile.readUTF();
				databaseFile.writeByte((byte) 4);	
			}
						
			try (ObjectOutputStream dataPositionInFileOutput = new ObjectOutputStream(new FileOutputStream("./db/indexes/status.ser"))){
				dataPositionInFileOutput.writeObject(dataPositionInFileStatus);
			}
		} catch(ClassNotFoundException e) {
		} catch(NullPointerException e) {
		} catch(FileNotFoundException e) {
		} catch(IOException e) {
			return true;
		}
		
		return false;
	}
	
	public boolean readRecords(CLI main) {
		try(RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "r")) {
			while(databaseFile.getFilePointer() < databaseFile.length() - 4) {
				main.printRecord(
					databaseFile.readInt(),
					databaseFile.readUTF(),
					databaseFile.readUTF(),
					databaseFile.readUTF(),
					databaseFile.readLong(),
					databaseFile.readByte(),
					databaseFile.readInt(),
					databaseFile.readUTF(),
					databaseFile.readInt(),
					databaseFile.readUTF(),
					databaseFile.readByte(),
					databaseFile.readUTF()
				);
			}
		} catch(FileNotFoundException e) {
		} catch(IOException e) {
			return true;
		}
		
		return false;
	}
	
	public boolean exportCSV(String pathCSV) {
		try(
				RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "r");
				BufferedWriter fileCSV = new BufferedWriter(new FileWriter(pathCSV))
		) {
			fileCSV.write("Name,Surname,Certificate expiration date,Card number,Card type,Contract number,Certificate serial number,Telephone,Email,Status,Note\n");
			while(databaseFile.getFilePointer() < databaseFile.length() - 4) {
				databaseFile.readInt();
				fileCSV.write(databaseFile.readUTF() + ",");
				fileCSV.write(databaseFile.readUTF() + ",");
				fileCSV.write(databaseFile.readUTF() + ",");
				fileCSV.write(databaseFile.readLong() + ",");
				switch(databaseFile.readByte()) {
					case 1:
						fileCSV.write("physical,");
						break;
					case 2:
						fileCSV.write("virtual,");
						break;
					default:
						fileCSV.write("other,");
						break;
				}
				fileCSV.write(databaseFile.readInt() + ",");
				fileCSV.write("===6FC0938=" + databaseFile.readUTF() + "=96E94AF34432===" + ",");
				fileCSV.write("+48 " + databaseFile.readInt() + ",");
				fileCSV.write(databaseFile.readUTF() + ",");
				switch(databaseFile.readByte()) {
				case 1:
					fileCSV.write("new,");
					break;
				case 2:
					fileCSV.write("proforma sent,");
					break;
				case 3:
					fileCSV.write("paid,");
					break;
				case 4:
					fileCSV.write("expired,");
					break;
				case 5:
					fileCSV.write("renewed,");
					break;
				default:
					fileCSV.write("other,");
					break;
				}
				fileCSV.write(databaseFile.readUTF());
				fileCSV.write("\n");
			}
		} catch(FileNotFoundException e) {
			return true;
		} catch(IOException e) {
			return true;
		}
		
		return false;
	}
	
	public boolean importCSV(CLI main, String pathCSV) {
		int line = 0;
		try(
				RandomAccessFile databaseFile = new RandomAccessFile(databaseFilePath, "rw");
				BufferedReader fileCSV = new BufferedReader(new FileReader(pathCSV))
		) {
			line = 1;
			fileCSV.readLine();
			SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			formatter.setLenient(false);
			formatter.setTimeZone(TimeZone.getTimeZone("Europe/Warsaw"));
			String row = "";
			while((row = fileCSV.readLine()) != null) {
				++line;
				String[] array = row.split(",");
				formatter.parse(array[2].trim());
				if(
					!(
						(
							array[0].trim().length() > 2 && array[0].trim().length() < 30
						) &&
						(
							array[1].trim().length() > 2 && array[1].trim().length() < 35
						) &&
						(
							array[3].trim().matches("^\\d{16}$")
						) &&
						(
							array[6].trim().matches("^===6FC0938=\\S{64}=96E94AF34432===$")
						) &&
						(
							array[7].trim().matches("^(\\+48 )\\d{9}$")
						) &&
						(
							array[8].trim().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")
						)
					)
				) {
					main.printCSVImportError("ERROR! Found error in line " + line + " of imported CSV file. Imported to " + line + " line. Import failure...");
					return true;
				}
				
				byte cardType = 0;
				switch(array[4].trim()) {
					case "physical":
						cardType = 1;
						break;
					case "virtual":
						cardType = 2;
						break;
					default:
						cardType = 3;
				}
				
				byte status = 0;
				switch(array[9].trim()) {
					case "new":
						status = 1;
						break;
					case "proforma sent":
						status = 2;
						break;
					case "paid":
						status = 3;
						break;
					case "expired":
						status = 4;
						break;
					case "renewed":
						status = 5;
						break;
					default:
						status = 6;
				}
				
				if (
					insertData(
							array[0].trim(),
							array[1].trim(),
							array[2].trim(),
							Long.parseLong(array[3].trim()),
							cardType, // 1-physical, 2-virtual, 3-other
							Integer.parseInt(array[5].trim()),
							array[6].trim().substring(11,75),
							Integer.parseInt(array[7].trim().substring(4)),
							array[8].trim(),
							status, // 1-new, 2-proforma sent, 3-paid, 4-expired, 5-renewed, 6-other
							array[10].trim()
					)
				) {
					return true;
				}
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			main.printCSVImportError("ERROR! Found error in line " + line + " of imported CSV file. Import failure...");
			return true;
		} catch(ParseException | NumberFormatException e) {
			main.printCSVImportError("ERROR! Found error in line " + line + " of imported CSV file. Import failure...");
			return true;
		} catch(EOFException e){
		} catch(FileNotFoundException e) {
			return true;
		} catch(IOException e) {
			return true;
		}
		
		return false;
	}
	
	public<T> boolean readRecord(CLI main, byte attribute, T value) throws NotFoundInDatabaseException {
		RandomAccessFile databaseFile = null;
		try {
			List<Long> positions = null;

			switch(attribute) {
				case 1:
					positions = getPositionFromIndex("./db/indexes/id.ser", value);
					break;
				case 2:
					positions = getPositionFromIndex("./db/indexes/name.ser", ((String) value).toLowerCase());
					break;
				case 3:
					positions = getPositionFromIndex("./db/indexes/surname.ser", ((String) value).toLowerCase());
					break;
				case 4:
					SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
					formatter.setTimeZone(TimeZone.getTimeZone("Europe/Warsaw"));
					Date date = null;
					try {
						date = formatter.parse((String) value);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					positions = getPositionFromIndex("./db/indexes/certificateExpirationDate.ser", date);
					break;
				case 5:
					positions = getPositionFromIndex("./db/indexes/cardNumber.ser", value);
					break;
				case 6:
					positions = getPositionFromIndex("./db/indexes/cardType.ser", value);
					break;
				case 7:
					positions = getPositionFromIndex("./db/indexes/contractNumber.ser", value);
					break;
				case 8:
					positions = getPositionFromIndex("./db/indexes/certificateSerialNumber.ser", ((String) value).toLowerCase());
					break;
				case 9:
					positions = getPositionFromIndex("./db/indexes/telephone.ser", value);
					break;
				case 10:
					positions = getPositionFromIndex("./db/indexes/email.ser", ((String) value).toLowerCase());
					break;
				case 11:
					positions = getPositionFromIndex("./db/indexes/status.ser", value);
					break;
				default:
					throw new FileNotFoundException();
			}
			
			if(positions != null && positions.size() != 0) {
				databaseFile = new RandomAccessFile(databaseFilePath, "r");
				for(Long pos : positions) {
					databaseFile.seek(pos);
					main.printRecord(
							databaseFile.readInt(),
							databaseFile.readUTF(),
							databaseFile.readUTF(),
							databaseFile.readUTF(),
							databaseFile.readLong(),
							databaseFile.readByte(),
							databaseFile.readInt(),
							databaseFile.readUTF(),
							databaseFile.readInt(),
							databaseFile.readUTF(),
							databaseFile.readByte(),
							databaseFile.readUTF()
						);
				}
				if(databaseFile != null) databaseFile.close();
			} else {
				throw new NotFoundInDatabaseException();
			}
		} catch(FileNotFoundException e) {
		} catch(IOException e) {
			e.printStackTrace();
			return true;
		}
		
		return false;
	}
	
	private<T> List<Long> getPositionFromIndex(String filePath, T value) throws FileNotFoundException, IOException {
		SortedMap<T, List<Long>> index = null;

		try (ObjectInputStream dataPositionInFileInput = new ObjectInputStream(new FileInputStream(filePath))){
			index = (SortedMap<T, List<Long>>) dataPositionInFileInput.readObject();			
		} catch (ClassNotFoundException e) {
			throw new FileNotFoundException();
		}
		
		return index.get(value);
	}
	
	public boolean readExpiringRecords(CLI main) {
		SortedMap<Date, List<Long>> index = null;
		RandomAccessFile databaseFile = null;
		try {
			try (ObjectInputStream dataPositionInFileInput = new ObjectInputStream(new FileInputStream("./db/indexes/certificateExpirationDate.ser"))){
				TimeZone tz = TimeZone.getTimeZone("Europe/Warsaw");
				Calendar c = Calendar.getInstance(tz);
				Date date = new Date();
		        c.setTime(date);
		        c.add(Calendar.MONTH, 2);
		        index = ((SortedMap<Date, List<Long>>) dataPositionInFileInput.readObject()).subMap(date, c.getTime());			
			} catch (ClassNotFoundException e) {
				throw new FileNotFoundException();
			}
			
			Collection<List<Long>> positions = index.values();
			if(positions.toArray().length != 0 && ((List<Long>) positions.toArray()[0]).size() != 0) {
				databaseFile = new RandomAccessFile(databaseFilePath, "r");
				for(List<Long> list: positions) { 
					for(Long pos : list) {
						databaseFile.seek(pos);
						main.printRecord(
								databaseFile.readInt(),
								databaseFile.readUTF(),
								databaseFile.readUTF(),
								databaseFile.readUTF(),
								databaseFile.readLong(),
								databaseFile.readByte(),
								databaseFile.readInt(),
								databaseFile.readUTF(),
								databaseFile.readInt(),
								databaseFile.readUTF(),
								databaseFile.readByte(),
								databaseFile.readUTF()
							);
					}
				}
				if(databaseFile != null) databaseFile.close();
			}
		} catch(FileNotFoundException e) {
		} catch(IOException e) {
			e.printStackTrace();
			return true;
		}
		
		return false;
	}
	
	private boolean updateIndexes() {
		if(updateIndex("./db/indexes/id.ser", this.id, this.dataPositionInFile)) return true;
		if(updateIndex("./db/indexes/name.ser", this.name.toLowerCase(), this.dataPositionInFile)) return true;
		if(updateIndex("./db/indexes/surname.ser", this.surname.toLowerCase(), this.dataPositionInFile)) return true;
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("Europe/Warsaw"));
		Date date = null;
		try {
			date = formatter.parse(this.certificateExpirationDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if(updateIndex("./db/indexes/certificateExpirationDate.ser", date, this.dataPositionInFile)) return true;
		if(updateIndex("./db/indexes/cardNumber.ser", this.cardNumber, this.dataPositionInFile)) return true;
		if(updateIndex("./db/indexes/cardType.ser", this.cardType, this.dataPositionInFile)) return true;
		if(updateIndex("./db/indexes/contractNumber.ser", this.contractNumber, this.dataPositionInFile)) return true;
		if(updateIndex("./db/indexes/certificateSerialNumber.ser", this.certificateSerialNumber.toLowerCase(), this.dataPositionInFile)) return true;
		if(updateIndex("./db/indexes/telephone.ser", this.telephone, this.dataPositionInFile)) return true;
		if(updateIndex("./db/indexes/email.ser", this.email.toLowerCase(), this.dataPositionInFile)) return true;
		if(updateIndex("./db/indexes/status.ser", this.status, this.dataPositionInFile)) return true;
		
		return false;
	}
	
	private<T extends Object & Comparable<T>> boolean updateIndex(String filePath, T value, long dataPositionInFile) {
		SortedMap<T, List<Long>> index = null;

		try (ObjectInputStream dataPositionInFileInput = new ObjectInputStream(new FileInputStream(filePath))){
			index = (SortedMap<T, List<Long>>) dataPositionInFileInput.readObject();			
		} catch(FileNotFoundException e) {
			index = new TreeMap<T, List<Long>>(new CustomComparator<T>());
		} catch(ClassNotFoundException e) {
			return true;
		} catch(IOException e) {
			return true;
		}

		List<Long> positions = null;
		if((positions = index.get(value)) != null) {
			positions.add(this.dataPositionInFile);
		} else {
			positions = new ArrayList<Long>();
			positions.add(this.dataPositionInFile);
			index.put(value, positions);
		}
		
		try (ObjectOutputStream dataPositionInFileOutput = new ObjectOutputStream(new FileOutputStream(filePath))){
			dataPositionInFileOutput.writeObject(index);
		} catch(FileNotFoundException e) {
		} catch(IOException e) {
			return true;
		}
		
		return false;
	}
}
