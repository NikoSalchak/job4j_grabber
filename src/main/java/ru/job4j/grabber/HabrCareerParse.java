package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;

public class HabrCareerParse {
    private static final String SOURCE_LINK = "http://career.habr.com";
    public static final String PREFIX = "/vacancies?page=";
    public static final String SUFFIX = "&q=Java%20developer&type=all";
    public static final int PAGES = 5;

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
        for (Element element : rows.select("p")) {
            sb.append(element.text()).append(ln);
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        int pageNumber = 1;
        while (pageNumber <= PAGES) {
            String fullLink = "%s%s%d%s".formatted(SOURCE_LINK, PREFIX, pageNumber, SUFFIX);
            Connection connection = Jsoup.connect(fullLink);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element titleDate = row.select(".vacancy-card__date").first();
                Element linkElement = titleElement.child(0);
                Element date = titleDate.child(0);
                String vacancyName = titleElement.text();
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                String description = new HabrCareerParse().retrieveDescription(link);
                System.out.printf("%s %s %s %s%n", vacancyName,
                        new HabrCareerDateTimeParser().parse(date.attr("datetime")), link, description);
            });
            pageNumber++;
        }
    }
}
