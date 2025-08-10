package io.github.sandydunlop.markista.structure;

public class SVGIcon {
    Kind kind;

    enum Kind {
        MODULE,
        PACKAGE,
        CODE,
        DOC,
        FOLDER
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

    public static SVGIcon code() {
        return new SVGIcon(Kind.CODE);
    }

    public static SVGIcon folder() {
        return new SVGIcon(Kind.FOLDER);
    }

    public static SVGIcon doc() {
        return new SVGIcon(Kind.DOC);
    }

    public Kind getKind() {
        return kind;
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
        } else if (kind == Kind.CODE) {
            sb.append(String.format("      <svg xmlns=\"http://www.w3.org/2000/svg\" x=\"%d\" y=\"%d\" fill=\"none\" viewBox=\"4 2 16 20\" height=\"12\" width=\"14\">\n", x, y));
            sb.append("        <path style=\"stroke: light-dark(#505050, #B9B5B4);\" stroke-linejoin=\"round\" stroke-linecap=\"round\" stroke-width=\"2\" d=\"M13 3.00087C12.9045 3 12.7973 3 12.6747 3H8.2002C7.08009 3 6.51962 3 6.0918 3.21799C5.71547 3.40973 5.40973 3.71547 5.21799 4.0918C5 4.51962 5 5.08009 5 6.2002V17.8002C5 18.9203 5 19.4801 5.21799 19.9079C5.40973 20.2842 5.71547 20.5905 6.0918 20.7822C6.51921 21 7.079 21 8.19694 21L15.8031 21C16.921 21 17.48 21 17.9074 20.7822C18.2837 20.5905 18.5905 20.2842 18.7822 19.9079C19 19.4805 19 18.9215 19 17.8036V9.32568C19 9.20296 19 9.09561 18.9991 9M13 3.00087C13.2856 3.00347 13.4663 3.01385 13.6388 3.05526C13.8429 3.10425 14.0379 3.18526 14.2168 3.29492C14.4186 3.41857 14.5918 3.59182 14.9375 3.9375L18.063 7.06298C18.4089 7.40889 18.5809 7.58136 18.7046 7.78319C18.8142 7.96214 18.8953 8.15726 18.9443 8.36133C18.9857 8.53376 18.9963 8.71451 18.9991 9M13 3.00087V5.8C13 6.9201 13 7.47977 13.218 7.90759C13.4097 8.28392 13.7155 8.59048 14.0918 8.78223C14.5192 9 15.079 9 16.1969 9H18.9991M18.9991 9H19.0002M14 13L16 15L14 17M10 17L8 15L10 13\" id=\"Vector\"/>\\n" + //
                                "");
        } else if (kind == Kind.FOLDER) {
            sb.append(String.format("      <svg xmlns=\"http://www.w3.org/2000/svg\" x=\"%d\" y=\"%d\" fill=\"none\" viewBox=\"2 4 20 16\" height=\"12\" width=\"14\">\n", x, y));
            sb.append("        <path style=\"stroke: light-dark(#505050, #B9B5B4);\" stroke-linejoin=\"round\" stroke-linecap=\"round\" stroke-width=\"2\" stroke=\"#000000\" d=\"M3 8.2C3 7.07989 3 6.51984 3.21799 6.09202C3.40973 5.71569 3.71569 5.40973 4.09202 5.21799C4.51984 5 5.0799 5 6.2 5H9.67452C10.1637 5 10.4083 5 10.6385 5.05526C10.8425 5.10425 11.0376 5.18506 11.2166 5.29472C11.4184 5.4184 11.5914 5.59135 11.9373 5.93726L12.0627 6.06274C12.4086 6.40865 12.5816 6.5816 12.7834 6.70528C12.9624 6.81494 13.1575 6.89575 13.3615 6.94474C13.5917 7 13.8363 7 14.3255 7H17.8C18.9201 7 19.4802 7 19.908 7.21799C20.2843 7.40973 20.5903 7.71569 20.782 8.09202C21 8.51984 21 9.0799 21 10.2V15.8C21 16.9201 21 17.4802 20.782 17.908C20.5903 18.2843 20.2843 18.5903 19.908 18.782C19.4802 19 18.9201 19 17.8 19H6.2C5.07989 19 4.51984 19 4.09202 18.782C3.71569 18.5903 3.40973 18.2843 3.21799 17.908C3 17.4802 3 16.9201 3 15.8V8.2Z\"/>\n");
                                
        } else {
            sb.append(String.format("      <svg xmlns=\"http://www.w3.org/2000/svg\" x=\"%d\" y=\"%d\" fill=\"none\" viewBox=\"4 2 16 20\" height=\"12\" width=\"14\">\n", x, y));
            sb.append("        <path style=\"stroke: light-dark(#505050, #B9B5B4);\" stroke-linejoin=\"round\" stroke-linecap=\"round\" stroke-width=\"2\" stroke=\"#000000\" d=\"M9 17H15M9 14H15M13.0004 3.00087C12.9048 3 12.7974 3 12.6747 3H8.2002C7.08009 3 6.51962 3 6.0918 3.21799C5.71547 3.40973 5.40973 3.71547 5.21799 4.0918C5 4.51962 5 5.08009 5 6.2002V17.8002C5 18.9203 5 19.4801 5.21799 19.9079C5.40973 20.2842 5.71547 20.5905 6.0918 20.7822C6.51921 21 7.079 21 8.19694 21L15.8031 21C16.921 21 17.48 21 17.9074 20.7822C18.2837 20.5905 18.5905 20.2842 18.7822 19.9079C19 19.4805 19 18.9215 19 17.8036V9.32568C19 9.20302 18.9999 9.09553 18.999 9M13.0004 3.00087C13.2858 3.00348 13.4657 3.01407 13.6382 3.05547C13.8423 3.10446 14.0379 3.18526 14.2168 3.29492C14.4186 3.41857 14.5918 3.59181 14.9375 3.9375L18.063 7.06298C18.4089 7.40889 18.5809 7.58136 18.7046 7.78319C18.8142 7.96214 18.8953 8.15726 18.9443 8.36133C18.9857 8.53379 18.9964 8.71454 18.999 9M13.0004 3.00087L13 5.80021C13 6.92031 13 7.48015 13.218 7.90797C13.4097 8.2843 13.7155 8.59048 14.0918 8.78223C14.5192 9 15.079 9 16.1969 9H18.999\"/>\n");
        }
        sb.append("\n      </svg>\n");
        return sb.toString();
    }
}
