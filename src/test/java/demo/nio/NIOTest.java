package demo.nio;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Stream;

public class NIOTest {

    private static String[] strs =
                {"A random string value",
                        "The product of an infinite number of monkeys",
                        "Hey hey we're the monkees",
                        "Opening act for the Monkees:Jimi Hendrix",
                        "Scuse me while I kiss this fly",
                        "Help Me! Help Me!"
                };

    private static int index = 0;

    private static boolean fillBuffer(CharBuffer charBuffer) {
        if (index >= strs.length) {
            return false;
        }
        for (int i = 0; i < strs[index].length(); i++) {
            charBuffer.put(strs[index].charAt(i));
        }
        index++;
        return true;
    }

    private static void drainBuffer(CharBuffer charBuffer) {
        while (charBuffer.hasRemaining()) {
            System.out.print(charBuffer.get());
        }
        System.out.println("");
    }

    public static void main(String[] args) {
        CharBuffer buffer = CharBuffer.allocate(100);
        while (fillBuffer(buffer)) {
            //每次读buffer之前都要flip()，将limit=position,position=0
            buffer.flip();
            drainBuffer(buffer);
            //读完之后clear()，该方法并没有清空buffer，而是将postion=0,limit=capacity
            buffer.clear();
        }
    }

    @Test
    public void testFiles() {
        Path path = Paths.get("d:/", "hello.txt");
        try {
            if(!Files.exists(path)) {
                System.out.println("文件不存在，创建新的文件");
                Files.createFile(path);
            }
            BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            String[] strs = {"yuding", "java", "NIO", "springMVC"};
            for(int i = 0; i < strs.length; i++) {
                bw.write(strs[i]);
                bw.newLine();
            }
            bw.close();

            BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            String tmp;
            while((tmp = br.readLine()) != null) {
                System.out.println(tmp);
            }
            br.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFile() {
        try {
            BufferedReader br = Files.newBufferedReader(Paths.get("d:/hello.txt"));
            String tmp = null;
            while((tmp = br.readLine()) != null) {
                System.out.println(tmp);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDirInterator() {
        long startTime = System.currentTimeMillis();
        Path path = Paths.get("d:/deploy");
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for(Path p : stream) {
                System.out.println(p.getFileName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("耗时：" + (System.currentTimeMillis() - startTime) / 1000 + "s");
    }

    @Test
    public void testList() {
        long startTime = System.currentTimeMillis();
        Path path = Paths.get("d:/deploy");
        try(Stream<Path> stream = Files.list(path)) {
            Iterator<Path> iterator = stream.iterator();
            while(iterator.hasNext()) {
                System.out.println(iterator.next().getFileName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("耗时：" + (System.currentTimeMillis() - startTime) / 1000 + "s");
    }

    @Test
    public void testWalkFileTree() {
        long startTime = System.currentTimeMillis();
        Path path = Paths.get("d:/deploy");
        LinkedList<Path> result = new LinkedList<Path>();
        try {
            Files.walkFileTree(path, new FindJarFileVistor(result));
            System.out.println(path.getFileName() + "目录下总共有" + result.size() + "个jar包");
            for(Path p : result) {
                System.out.println(p.getFileName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("耗时：" + (System.currentTimeMillis() - startTime) / 1000 + "s");
    }

    private class FindJarFileVistor extends SimpleFileVisitor<Path> {

        private LinkedList<Path> result;

        public FindJarFileVistor(LinkedList<Path> result) {
            this.result = result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if(file.toString().endsWith(".jar")) {
                result.add(file);
            }
            return FileVisitResult.CONTINUE;
        }
    }

    @Test
    public void testCopy() {
        try {
            Files.copy(Paths.get("d:/hello.txt"), System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFilesApi() throws IOException {
        Path path = Paths.get("d:/hello.txt");
        System.out.println(Files.isDirectory(path));
        System.out.println(Files.getLastModifiedTime(path));
        System.out.println(Files.isSymbolicLink(path));
        System.out.println(Files.readAttributes(path, "*"));
    }
}
