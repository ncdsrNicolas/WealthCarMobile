package br.edu.fatecpg.wealthcar_teste

object CarImageHelper {
    private const val SUPABASE_URL = "https://mcvewawvriontqggrzfg.supabase.co"
    private const val BUCKET = "car-images"

    fun getUrl(marca: String, modelo: String): String {
        val slug = "$marca-$modelo"
            .lowercase()
            .replace("\\s+".toRegex(), "-")
            .replace("[^a-z0-9-]".toRegex(), "")
        return "$SUPABASE_URL/storage/v1/object/public/$BUCKET/$slug.png"
    }
}
// Exemplo: "Mitsubishi", "Lancer Evolution", 2014
// → "mitsubishi-lancer-evolution-2014.png"