package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;
import ru.job4j.grabber.utils.Post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "http://career.habr.com";
    public static final String PREFIX = "/vacancies?page=";
    public static final String SUFFIX = "&q=Java%20developer&type=all";
    public static final int PAGES = 5;
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private String retrieveDescription(String link) {
        StringBuilder sb = new StringBuilder();
        String ln = System.lineSeparator();
        Connection connection = Jsoup.connect(link);
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
        Element titleDate = row.select(".vacancy-card__date").first();
        Element linkElement = titleElement.child(0);
        Element date = titleDate.child(0);
        String vacancyName = titleElement.text();
        String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
        String description = retrieveDescription(link);
        post.setTitle(vacancyName);
        post.setLink(link);
        post.setDescription(description);
        post.setCreated(dateTimeParser.parse(date.attr("datetime")));
        return post;
    }

    @Override
    public List<Post> list(String link) {
        int pageNumber = 1;
        List<Post> listPosts = new ArrayList<>();
        while (pageNumber <= PAGES) {
            String fullLink = "%s%s%d%s".formatted(link, PREFIX, pageNumber, SUFFIX);
            Connection connection = Jsoup.connect(fullLink);
            Document document = null;
            try {
                document = connection.get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Elements rows = document.select(".vacancy-card__inner");
            rows.stream().map(this::createPost).forEach(listPosts::add);
            pageNumber++;
        }
        return listPosts;
    }

    public static void main(String[] args) {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        for (Post post : habrCareerParse.list("http://career.habr.com")) {
            System.out.println(post);
        }
    }
}
