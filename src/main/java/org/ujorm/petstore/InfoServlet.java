package org.ujorm.petstore;

import jakarta.servlet.annotation.WebServlet;
import org.ujorm.petstore.Constants.Css;
import org.ujorm.petstore.Layout.NavItem;
import org.ujorm.petstore.utilities.AbstractServlet;
import org.ujorm.tools.markdown.MarkdownToHtmlConverter;
import org.ujorm.tools.web.Element;
import org.ujorm.tools.web.HtmlElement;
import org.ujorm.tools.web.request.HttpContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.ujorm.petstore.Constants.*;

/** Project information page rendered from a Markdown source. */
@WebServlet(urlPatterns = Url.INFO, loadOnStartup = 1)
public class InfoServlet extends AbstractServlet {

    private static final String INFO_SOURCE = "/info.md";
    private static final String PAGE_TITLE = Layout.BRAND + " — About";

    private final MarkdownToHtmlConverter markdownToHtmlConverter = new MarkdownToHtmlConverter();

    @Override
    protected void doGet(HttpContext ctx) throws IOException {
        try (var html = HtmlElement.of(PAGE_TITLE, ctx, Layout.BOOTSTRAP_CSS)) {
            Layout.addCommonStyles(html);
            html.addCssBody(articleStyles());
            try (var body = html.addBody(Css.container, Css.mt5)) {
                Layout.renderHeader(body, ctx, NavItem.ABOUT);
                renderContent(body);
            }
        }
    }

    /** Renders the markdown content inside a styled article block. */
    private void renderContent(Element body) throws IOException {
        try (var article = body.addElement("article", Css.article)) {
            markdownToHtmlConverter.render(article, loadMarkdownSource());
        }
    }

    private String loadMarkdownSource() throws IOException {
        try (var is = getClass().getResourceAsStream(INFO_SOURCE)) {
            if (is == null) {
                throw new IOException("Resource not found: " + INFO_SOURCE);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Page-specific styling for the Markdown article. Layers on top of
     * Bootstrap so the document stays visually consistent with the catalog
     * page (same primary colour, same spacing scale, same neutrals).
     */
    private String articleStyles() {
        return """
                article.article {
                    margin: 0 0 3rem;
                    background: #fff;
                    border-radius: 14px;
                    padding: 1.75rem 2rem;
                    box-shadow: 0 10px 30px rgba(15, 23, 42, .06);
                    color: #334155;
                    line-height: 1.7;
                }
                article.article h1,
                article.article h2,
                article.article h3 { color: #0d6efd; }
                article.article h1 { font-size: 1.9rem; margin: .25rem 0 1rem; }
                article.article h2 {
                    font-size: 1.4rem;
                    margin: 2rem 0 .75rem;
                    padding-bottom: .35rem;
                    border-bottom: 1px solid #e2e8f0;
                }
                article.article h3 { font-size: 1.15rem; margin: 1.25rem 0 .5rem; }
                article.article p { margin-bottom: 1rem; }
                article.article ul, article.article ol { padding-left: 1.4rem; }
                article.article li { margin: .25rem 0; }
                article.article a { color: #0d6efd; text-decoration: none; }
                article.article a:hover { text-decoration: underline; }
                article.article code {
                    background: #f1f5f9;
                    color: #be185d;
                    padding: .12em .35em;
                    border-radius: .25rem;
                    font-size: .9em;
                }
                article.article pre {
                    background: #0f172a;
                    color: #e2e8f0;
                    padding: 1rem 1.25rem;
                    border-radius: .5rem;
                    overflow-x: auto;
                }
                article.article pre code {
                    background: transparent;
                    color: inherit;
                    padding: 0;
                    font-size: .9rem;
                }
                article.article blockquote {
                    border-left: 4px solid #93c5fd;
                    background: #f8fafc;
                    margin: 1rem 0;
                    padding: .6rem 1rem;
                    color: #475569;
                    border-radius: 0 .35rem .35rem 0;
                }
                article.article hr {
                    border: 0;
                    border-top: 1px solid #e2e8f0;
                    margin: 2rem 0;
                }
                article.article table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 1rem 0;
                }
                article.article th, article.article td {
                    border: 1px solid #e2e8f0;
                    padding: .5rem .75rem;
                    text-align: left;
                }
                article.article th { background: #f1f5f9; color: #0f172a; }
                article.article img { max-width: 100%; height: auto; }
                """;
    }
}
