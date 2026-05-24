package br.edu.fatecpg.wealthcar_teste

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://mcvewawvriontqggrzfg.supabase.co",
        supabaseKey = "sb_publishable_lu6aSbIeiNu49dRCuV748w_lyet37VX"
    ) {
        install(Auth)
        install(Postgrest)
    }
}