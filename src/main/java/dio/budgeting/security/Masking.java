package dio.budgeting.security;

/** Esconde dado pessoal nos logs: o log e guardado e lido por gente que nao precisa ver o numero inteiro. */
public final class Masking {

    private Masking() {
    }

    /** "5519999998888" -> "***8888". */
    public static String phone(String phone) {
        return phone == null || phone.length() < 4 ? "***" : "***" + phone.substring(phone.length() - 4);
    }
}
