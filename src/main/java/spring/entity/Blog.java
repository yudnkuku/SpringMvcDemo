package spring.entity;

import java.util.List;

public class Blog {

    private int id;

    private String name;

    private Author author;

    private List<Article> articles;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public List<Article> getArticles() {
        return articles;
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Article article : articles) {
            sb.append(article.getContent()).append("\n");
        }
        return "Blog{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", author=" + author +
                ", articles content=" + sb.toString() +
                '}';
    }
}
