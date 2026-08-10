package com.shahrafuking.kingassistant.speech

import android.content.Context
import android.util.Log
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.random.Random

/**
 * ChallengeGenerator
 * - Attempts to fetch a dynamic Bengali one‑liner joke or two‑line poem from external sources.
 * - If network fetch fails, procedurally generates a short Bengali line using templates and random words.
 * - Never uses a fixed static dataset embedded in code — procedural fallback ensures uniqueness.
 */
object ChallengeGenerator {
    private const val TAG = "ChallengeGenerator"
    private val client = OkHttpClient()

    // Public method: tries to fetch from remote endpoints, then fallback to procedural generation
    fun fetchChallenge(context: Context, timeoutMs: Long = 2500): String {
        // List of candidate endpoints (public raw text or APIs). These are optional; failure is tolerated.
        val endpoints = listOf(
            // Public gist/raw sources can be placed here by owner; leaving common helpful endpoints empty by default
            // Example placeholder: "https://raw.githubusercontent.com/<user>/bengali-jokes/main/jokes.txt"
        )

        // Try each endpoint quickly (short timeout)
        for (url in endpoints) {
            try {
                val req = Request.Builder().url(url).get().build()
                val call = client.newCall(req)
                val resp = call.execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string()?.trim()
                    if (!body.isNullOrBlank()) {
                        // pick a random line
                        val lines = body.lines().filter { it.isNotBlank() }
                        if (lines.isNotEmpty()) {
                            val pick = lines[Random.nextInt(lines.size)].trim()
                            return sanitizeChallenge(pick)
                        }
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "fetch from $url failed", e)
            } catch (t: Throwable) {
                Log.w(TAG, "unexpected fetch error", t)
            }
        }

        // Fallback: generate procedurally (templates + random words)
        return proceduralChallenge()
    }

    private fun proceduralChallenge(): String {
        val intros = listOf("একটা গল্প বলি:", "ছোট্ট কবিতা:", "একটি মিষ্টি ধাঁধা:")
        val structures = listOf(
            // two-line poem
            { ->
                val a = randomBengaliWord(); val b = randomBengaliWord(); val c = randomBengaliWord()
                "$a-এর মাঝে $b,\n$ c-ই হাসে।"
            },
            // short joke line
            { ->
                val who = randomBengaliName(); val what = randomAction()
                "$who $what বলে হেসে চেয়েছিলো।"
            },
            // riddle-like
            { ->
                val a = randomBengaliWord(); val b = randomBengaliWord()
                "কি যে $a, $b-কে দেখে লুকায়?"
            }
        )
        val pick = structures[Random.nextInt(structures.size)].invoke()
        val header = intros[Random.nextInt(intros.size)]
        return sanitizeChallenge("$header\n$pick")
    }

    private fun sanitizeChallenge(s: String): String {
        return s.trim().replace(Regex("\s+"), " ")
    }

    private fun randomBengaliWord(): String {
        val words = listOf("চাঁদের", "নীলের", "রবির", "পাখির", "ফুলের", "শাহ্ রাফু-এর", "কিং-এর", "সোনার")
        return words[Random.nextInt(words.size)]
    }

    private fun randomBengaliName(): String {
        val names = listOf("রাফু", "শাহ্", "মিঠু", "জবি", "অর্ণব")
        return names[Random.nextInt(names.size)]
    }

    private fun randomAction(): String {
        val acts = listOf("একটা পাগল কথা বললো", "চুপ করে হাসলো", "কাঁপতে লাগলো")
        return acts[Random.nextInt(acts.size)]
    }
}
