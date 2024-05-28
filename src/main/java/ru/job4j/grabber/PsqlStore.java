package ru.job4j.grabber;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;
import ru.job4j.grabber.utils.Post;
import org.jsoup.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store {
    private Connection connection;
    private static final String SOURCE_LINK = "http://career.habr.com";
    public static final String PREFIX = "/vacancies?page=";
    public static final String SUFFIX = "&q=Java%20developer&type=all";
    private static final int PAGES = 5;

    public PsqlStore(Properties config) throws SQLException {
        try {
            Class.forName(config.getProperty("jdbc.driver"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        connection = DriverManager.getConnection(
                config.getProperty("jdbc.url"),
                config.getProperty("jdbc.username"),
                config.getProperty("jdbc.password")
                );
    }
    private String retrieveDescription(String link) {
        StringBuilder sb = new StringBuilder();
        String ln = System.lineSeparator();
        org.jsoup.Connection connection = Jsoup.connect(link);
        Document document = null;
        try {
            document = connection.get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Elements rows = document.select(".vacancy-description__text");
        Element title = rows.select("h3").first();
        sb.append(title.text()).append(ln);
        for (Element element : rows.select(".style-ugc")) {
            sb.append(element.text()).append(ln);
        }
        return sb.toString();
    }

    private Post createPost(Element row) {
        Post post = new Post();
        Element titleElement = row.select(".vacancy-card__title").first();
        String vacancyName = titleElement.text();
        String vacancyDate = row.select(".vacancy-card__date").first().child(0).attr("datetime");
        String link = String.format("%s%s", SOURCE_LINK, titleElement.child(0).attr("href"));
        String description = retrieveDescription(link);
        post.setTitle(vacancyName);
        post.setLink(link);
        post.setDescription(description);
        post.setCreated(new HabrCareerDateTimeParser().parse(vacancyDate));
        return post;
    }

    public void parseData() {
        int pageNumber = 1;
        while (pageNumber <= PAGES) {
            String fullLink = "%s%s%d%s".formatted(SOURCE_LINK, PREFIX, pageNumber, SUFFIX);
            org.jsoup.Connection jsoupConnect = Jsoup.connect(fullLink);
            Document document = null;
            try {
                document = jsoupConnect.get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Elements rows = document.select(".vacancy-card__inner");
            rows.stream().map(this::createPost).forEach(this::save);
            pageNumber++;
        }
    }

    private Post getPostFromDataBase(ResultSet resultSet) throws SQLException {
        return new Post(resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("link"),
                resultSet.getString("text"),
                resultSet.getTimestamp("created").toLocalDateTime());
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement = connection
                .prepareStatement(
                        "INSERT INTO post(name, text, link, created) VALUES(?, ?, ?, ?) ON CONFLICT (link) do nothing")) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM post")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(getPostFromDataBase(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    @Override
    public Post findById(int id) {
        Post post = new Post();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM post WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    post = getPostFromDataBase(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    public static void main(String[] args) {
        Properties config = new Properties();
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("rabbit.properties")) {
            config.load(in);
            try (PsqlStore psqlStore = new PsqlStore(config)) {
                psqlStore.parseData();
                for (Post post : psqlStore.getAll()) {
                    System.out.println(post);
                }
                System.out.println(psqlStore.findById(25));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
