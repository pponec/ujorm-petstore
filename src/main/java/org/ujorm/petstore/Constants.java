package org.ujorm.petstore;

import org.ujorm.tools.web.ao.HttpParameter;

/** Common constants */
public interface Constants {

    /** Pet statuses */
    enum Status implements HttpParameter {
        AVAILABLE,
        PENDING,
        SOLD;

        /** Finds status by string name */
        public static Status findByName(String name) {
            for (var status : values()) {
                if (status.equalsParamName(name)) {
                    return status;
                }
            }
            return AVAILABLE;
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /** CSS constants */
    final class Css {
        static final String alignItemsCenter = "align-items-center";
        static final String badge = "badge";
        static final String bgSecondary = "bg-secondary";
        static final String bgSuccess = "bg-success";
        static final String bgWarning = "bg-warning";
        static final String borderBottom = "border-bottom";
        static final String btn = "btn";
        static final String btnOutlineDanger = "btn-outline-danger";
        static final String btnOutlinePrimary = "btn-outline-primary";
        static final String btnPrimary = "btn-primary";
        static final String btnSm = "btn-sm";
        static final String btnSuccess = "btn-success";
        static final String colMd2 = "col-md-2";
        static final String colMd3 = "col-md-3";
        static final String colMd4 = "col-md-4";
        static final String container = "container";
        static final String dFlex = "d-flex";
        static final String dInline = "d-inline";
        static final String formControl = "form-control";
        static final String formSelect = "form-select";
        static final String g3 = "g-3";
        static final String justifyContentBetween = "justify-content-between";
        static final String mb3 = "mb-3";
        static final String mb4 = "mb-4";
        static final String ms1 = "ms-1";
        static final String mt5 = "mt-5";
        static final String pb3 = "pb-3";
        static final String row = "row";
        static final String table = "table";
        static final String tableDark = "table-dark";
        static final String tableHover = "table-hover";
        static final String textPrimary = "text-primary";
        static final String w100 = "w-100";
    }
}
