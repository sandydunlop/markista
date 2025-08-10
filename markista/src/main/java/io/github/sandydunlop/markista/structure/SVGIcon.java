package io.github.sandydunlop.markista.structure;

public class SVGIcon {
    Kind kind;

    enum Kind {
        MODULE,
        PACKAGE,
        FILE
    }

    public SVGIcon(Kind kind) {
        this.kind = kind;
    }

    public static SVGIcon module() {
        return new SVGIcon(Kind.MODULE);
    }

    public static SVGIcon pkg() {
        return new SVGIcon(Kind.PACKAGE);
    }

    public static SVGIcon file() {
        return new SVGIcon(Kind.FILE);
    }

    public String placeAt(int x, int y) {
        StringBuilder sb = new StringBuilder();
        if (kind == Kind.MODULE) {
            sb.append(String.format("      <svg xmlns=\"http://www.w3.org/2000/svg\" x=\"%d\" y=\"%d\" fill=\"none\" viewBox=\"2 3 20 18\" height=\"12\" width=\"14\">\n", x, y));
            sb.append("        <path style=\"stroke: light-dark(#505050, #B9B5B4);\" stroke-linejoin=\"round\" stroke-linecap=\"round\" stroke-width=\"2\" d=\"M21 14L12 20L3 14M21 10L12 16L3 10L12 4L21 10Z\"/>\n");
        } else if (kind == Kind.PACKAGE) {
            sb.append(String.format("      <svg xmlns=\"http://www.w3.org/2000/svg\" x=\"%d\" y=\"%d\" fill=\"none\" viewBox=\"1 1 22 22\" height=\"12\" width=\"14\">\n", x, y));
            sb.append("        <path style=\"fill: light-dark(#505050, #B9B5B4)\" d=\"M20.929,1.628A1,1,0,0,0,20,1H4a1,1,0,0,0-.929.628l-2,5A1.012,1.012,0,0,0,1,7V22a1,1,0,0,0,1,1H22a1,1,0,0,0,1-1V7a1.012,1.012,0,0,0-.071-.372ZM4.677,3H19.323l1.2,3H3.477ZM3,21V8H21V21Zm8-3a1,1,0,0,1-1,1H6a1,1,0,0,1,0-2h4A1,1,0,0,1,11,18Z\"/>\\n" + //
                                "");
        } else {
            sb.append(String.format("      <svg xmlns=\"http://www.w3.org/2000/svg\" x=\"%d\" y=\"%d\" fill=\"none\" viewBox=\"4 2 16 20\" height=\"12\" width=\"14\">\n", x, y));
            sb.append("        <path style=\"stroke: light-dark(#505050, #B9B5B4);\" stroke-linejoin=\"round\" stroke-linecap=\"round\" stroke-width=\"2\" d=\"M13 3.00087C12.9045 3 12.7973 3 12.6747 3H8.2002C7.08009 3 6.51962 3 6.0918 3.21799C5.71547 3.40973 5.40973 3.71547 5.21799 4.0918C5 4.51962 5 5.08009 5 6.2002V17.8002C5 18.9203 5 19.4801 5.21799 19.9079C5.40973 20.2842 5.71547 20.5905 6.0918 20.7822C6.51921 21 7.079 21 8.19694 21L15.8031 21C16.921 21 17.48 21 17.9074 20.7822C18.2837 20.5905 18.5905 20.2842 18.7822 19.9079C19 19.4805 19 18.9215 19 17.8036V9.32568C19 9.20296 19 9.09561 18.9991 9M13 3.00087C13.2856 3.00347 13.4663 3.01385 13.6388 3.05526C13.8429 3.10425 14.0379 3.18526 14.2168 3.29492C14.4186 3.41857 14.5918 3.59182 14.9375 3.9375L18.063 7.06298C18.4089 7.40889 18.5809 7.58136 18.7046 7.78319C18.8142 7.96214 18.8953 8.15726 18.9443 8.36133C18.9857 8.53376 18.9963 8.71451 18.9991 9M13 3.00087V5.8C13 6.9201 13 7.47977 13.218 7.90759C13.4097 8.28392 13.7155 8.59048 14.0918 8.78223C14.5192 9 15.079 9 16.1969 9H18.9991M18.9991 9H19.0002M14 13L16 15L14 17M10 17L8 15L10 13\" id=\"Vector\"/>\\n" + //
                                "");
        }
        sb.append("\n      </svg>\n");
        return sb.toString();
    }
}
