package org.ujorm.petstore;

import org.ujorm.petstore.Constants.Css;
import org.ujorm.tools.web.Element;
import org.ujorm.tools.web.HtmlElement;
import org.ujorm.tools.web.request.HttpContext;

/**
 * Shared page layout helpers used by all servlets — keeps the brand header,
 * navigation pills and logo identical across pages so individual servlets can
 * focus on their own content.
 */
public final class Layout {

    /** Bootstrap 5.3 CSS bundled from a CDN (single source for all pages). */
    public static final String BOOTSTRAP_CSS =
            "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css";

    /** Brand title displayed in the page header. */
    public static final String BRAND = "Ujorm PetStore";

    private Layout() { }

    /** A navigable page within the application. */
    public enum NavItem {
        /** Pet catalog — paw-prints glyph (U+1F43E). */
        PETS("Pets", Constants.Url.PETS, "\uD83D\uDC3E"),
        /** Project info — circled latin small letter "i" (U+24D8). */
        ABOUT("About", Constants.Url.INFO, "\u24D8");

        /** Visible label of the navigation item. */
        public final String label;
        /** Servlet URL pattern (see {@link Constants.Url}); may start with {@code /} or be empty. */
        public final String path;
        /** Single-character Unicode glyph used as a navigation icon. */
        public final String icon;

        NavItem(String label, String path, String icon) {
            this.label = label;
            this.path = path;
            this.icon = icon;
        }

        /**
         * Resolved absolute URL of this nav item. Combines the servlet
         * context path with {@link #path}, normalizing the slashes so that
         * patterns like {@code ""} (root) and {@code "/info"} both produce
         * valid hrefs regardless of how the application is deployed.
         */
        public String url(HttpContext ctx) {
            var ctxSlash = ctx.getPathSlash();                          // "/" or "/app/"
            var rel = path.startsWith("/") ? path.substring(1) : path;  // strip leading "/"
            return ctxSlash + rel;
        }
    }

    /**
     * Add the shared inline stylesheet to the document head. Must be called
     * <strong>before</strong> {@code html.addBody(...)} so the &lt;style&gt;
     * element is emitted inside &lt;head&gt;.
     */
    public static void addCommonStyles(HtmlElement html) {
        html.addCssBody(commonStyles());
    }

    private static String commonStyles() {
        return """
                body { background: #f8fafc; }
                .nav-pills .nav-link {
                    display: inline-flex;
                    align-items: center;
                    color: #0d6efd;
                    border: 1px solid transparent;
                    padding: .35rem .85rem;
                    transition: background-color .15s ease, border-color .15s ease;
                }
                .nav-pills .nav-link:hover {
                    background-color: rgba(13, 110, 253, .08);
                    border-color: rgba(13, 110, 253, .15);
                }
                .nav-pills .nav-link.active {
                    background-color: #0d6efd;
                    color: #fff;
                }
                .nav-pills .nav-link .nav-icon {
                    font-size: 1.1em;
                    line-height: 1;
                    margin-right: .4rem;
                }
                """;
    }

    /**
     * Render the shared application header: brand title, page-switcher
     * pills and logo. The layout is a flex row, so the navigation simply
     * fills the space between the title and the logo without changing
     * the original page geometry.
     */
    public static void renderHeader(Element body, HttpContext ctx, NavItem active) {
        var contextPath = ctx.getPathSlash();
        try (var header = body.addDiv(
                Css.dFlex,
                Css.justifyContentBetween,
                Css.alignItemsCenter,
                Css.flexWrap,
                Css.mb4,
                Css.borderBottom,
                Css.pb3)) {
            header.addHeading(1, BRAND, Css.textPrimary, Css.mb0);
            renderNav(header, ctx, active);
            header.addAnchor(contextPath)
                    .addImage(contextPath + Constants.IMG_LOGO, "Ujorm Logo")
                    .setAttr("width", 150)
                    .setAttr("height", 150);
        }
    }

    /** Render the Bootstrap pill navigation for switching between servlets. */
    private static void renderNav(Element parent, HttpContext ctx, NavItem active) {
        try (var nav = parent.addElement("ul", Css.nav, Css.navPills, Css.mx3, Css.gap2)) {
            for (var item : NavItem.values()) {
                try (var li = nav.addElement("li", Css.navItem)) {
                    var isActive = item == active;
                    var link = isActive
                            ? li.addAnchor(item.url(ctx), Css.navLink, Css.active)
                            : li.addAnchor(item.url(ctx), Css.navLink);
                    if (isActive) {
                        link.setAttr("aria-current", "page");
                    }
                    link.addElement("span", Css.navIcon).setAttr("aria-hidden", "true").addText(item.icon);
                    link.addText(item.label);
                }
            }
        }
    }
}
