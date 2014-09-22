package Archiver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.*;

/**
 *
 * @author fedor
 */
public class Archiver {

//    добавить файл или папку в указанный архив
    private static void addToZip(String srcZipFile, String[] files) {
        HashSet<String> fileSet = new HashSet();
        for (int i = 0; i < files.length; i++) {
            fileSet.add(files[i]);
        }
        try {
//        необходимые объекты-файлы
            File zipFile = new File(srcZipFile);
            if (!zipFile.exists()) {
                zipFile.createNewFile();
            }
//            File file = new File(srcFile);
//            промежуточный файл, в который потом заливается существующий архив
            File tempFile;
            tempFile = File.createTempFile(zipFile.getName(), "");
//            копирование архива в промежуточный файл
            Path path = null;
            path = Files.copy(zipFile.toPath(), tempFile.toPath(), REPLACE_EXISTING);
//            буфер для записи
            byte[] buf = new byte[1024];
//            поток из промежуточного файла, в котором лежит архив
            ZipInputStream zin;
            zin = new ZipInputStream(new FileInputStream(tempFile));
//            поток в начальный архив
            ZipOutputStream zout;
            zout = new ZipOutputStream(new FileOutputStream(zipFile));
//            объект для каждого входа в архив
            ZipEntry entry;
            entry = zin.getNextEntry();
//            для каждого входа выполним копирование, если файл не повторяется
            while (entry != null) {
                String name = entry.getName();
                boolean notSame = true;
//                если такой файл уже в архиве, выходим из цикла
                for (String s : fileSet) {
                    File file = new File(s);
                    if (file.getName().equals(name)) {
                        System.out.println("File already exists");
                        notSame = false;
                        break;
                    }
                }
//                если файла в архиве нет, выполняем копирование
                if (notSame) {
                    ZipEntry ze = new ZipEntry(name);
                    addZipEntry(zin, zout, ze);
                }
//                переходим к следующему входу в архив
                entry = zin.getNextEntry();
            }
//            закроем поток из промежуточного файла
            zin.close();
//            скопировали содержимое самого архива, добавим теперь сам файл
            for (String s : files) {
                addFileToZip(zout, parcePath(s)[0], parcePath(s)[1]);
            }
//            закроем выходной поток в архив 
            zout.close();
        } catch (IOException e) {
            System.out.println("Can't add object to archive:");
            System.out.println(e.toString());
        }
    }

//    извлечь архив в указанную папку
    private static void unzipArchive(String zipFile, String outputFolder) {
//        папка, в которую извлекается архив
        File folder = new File(outputFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }
//        входной поток из архива
        try {
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze;
//            пройдемся по всем входам в архив
            while ((ze = zin.getNextEntry()) != null) {
                unzipFile(zin, outputFolder, ze);
            }
            zin.close();
        } catch (IOException e) {
            System.out.println("Can't unzip archive:");
            System.out.println(e.toString());
        }
    }

//    разархивировать вход в архив
    private static void unzipFile(ZipInputStream zin, String outputFolder, ZipEntry ze) {
        String fileName = ze.getName();
        File unzippedFile = new File(outputFolder + File.separator + fileName);
        if (ze.isDirectory()) {
            unzippedFile.mkdirs();
        } else {
            new File(unzippedFile.getParent()).mkdirs();
            try {
                unzippedFile.createNewFile();
                byte[] buffer = new byte[1024];
//                выходной поток в файл
                FileOutputStream fout = new FileOutputStream(unzippedFile);
                int len;
                while ((len = zin.read(buffer)) > 0) {
                    fout.write(buffer, 0, len);
                }
                fout.close();
            } catch (IOException e) {
                System.out.println("Can't unzip file:");
                System.out.println(e.toString());
            }
        }
    }

//    разбить абсолютный адрес на директорию и файл
    private static String[] parcePath(String path) {
        List list = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(path, File.separator);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            list.add(token);
        }
        Object[] array = list.toArray();
        StringBuilder dir = new StringBuilder();
        int i;
        dir.append(File.separator);
        for (i = 0; i < array.length - 1; i++) {
            dir.append(array[i].toString()).append(File.separator);
        }
        String file = array[i].toString();
        String[] result = {dir.toString(), file};
        return result;
    }

//    добавить файл в нвоый архив
    private static void addFileToZip(ZipOutputStream zout, String srcDir, String fileName) {
        File folder = new File(srcDir + fileName);
//        проверяем, что файл - не директория
        if (folder.isDirectory()) {
            addFolderToZip(zout, srcDir, fileName);
        } else {
//            поток из добавляемого файла и новый вход в архив
            FileInputStream fin;
            try {
                fin = new FileInputStream(srcDir + fileName);
                ZipEntry ze = new ZipEntry(fileName);
//                заполняем вход
                addZipEntry(fin, zout, ze);
//                закрываем потоки
                fin.close();
            } catch (IOException e) {
                System.out.println("Can't add file to archive:");
                System.out.println(e.toString());
            }
        }
    }

//    добавить папку в архив
    private static void addFolderToZip(ZipOutputStream zout, String srcDir, String dirName) {
        File folder = new File(srcDir + dirName);
//        добавляем по очереди каждый файл из папки
        for (String fileName : folder.list()) {
            addFileToZip(zout, srcDir, dirName + File.separator + fileName);
        }
    }

//    добавить вход в архив
    private static void addZipEntry(InputStream is, ZipOutputStream zos, ZipEntry ze) {
        try {
//            объявляем новый вход
            zos.putNextEntry(ze);
//            буфер и длина считанной последовательности байтов
            byte[] buf = new byte[1024];
            int len;
//            записываем из входного потока в выходной
            while ((len = is.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
//            закрываем вход
            zos.closeEntry();
        } catch (IOException e) {
            System.out.println("Can't add entry to archive:");
            System.out.println(e.toString());
        }
    }

    private static void printInstruction() {
        System.out.println("ZIP Archiver v1.0");
        System.out.println("Usage: java Archiver -zip *archiveScr* *file1Src* *file2Src* ... or");
        System.out.println("       java Archiver -unzip *archiveScr* *folderSrc*");
    }

//    метод, запускающий все
    public void act(String[] args) {

        if (args.length < 3) {
            printInstruction();
        } else {
            String op = args[0];
            String archiveSrc = args[1];
            switch (op) {
                case "-zip":
                    String[] files = new String[args.length - 2];
                    for (int i = 0; i < args.length - 2; i++) {
                        files[i] = args[i + 2];
                    }
                    addToZip(archiveSrc, files);
                    break;
                case "-unzip":
                    unzipArchive(archiveSrc, args[2]);
                    break;
            }
        }
    }
    
//    собственно main
    public static void main(String[] args) {
        Archiver a = new Archiver();
        a.act(args);
    }
}
