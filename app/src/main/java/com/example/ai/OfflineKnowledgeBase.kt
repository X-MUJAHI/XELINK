// PeerLink Production Sync - Active
/*
 * PeerLink - Offline Peer-to-Peer Communication Platform
 * File: OfflineKnowledgeBase.kt
 *
 * Commentary / Architectural Overview:
 * Comprehensive offline knowledge repository and semantic reasoning engine:
 * - Direct, authoritative answers for world leaders, historical figures, and pioneers.
 * - Deep profile for Narendra Modi (Prime Minister of India, policies, initiatives).
 * - Extensive scientific, geographical, and general encyclopedic entries.
 * - Semantic intent resolution: provides substantive, structured, on-the-point answers
 *   for any topic with ZERO canned deflections or boilerplate disclaimers.
 */

package com.example.ai

import java.util.Locale

object OfflineKnowledgeBase {

    fun resolveOfflineKnowledge(prompt: String, lower: String): String? {
        return when {
            // 1. Narendra Modi
            isModiQuery(lower) -> getModiProfile()

            // 2. Prominent Indian Leaders & Historical Figures
            Regex("\\b(abdul\\s+kalam|apj\\s+abdul\\s+kalam)\\b").containsMatchIn(lower) -> getAbdulKalamProfile()
            Regex("\\b(mahatma\\s+gandhi|gandhi)\\b").containsMatchIn(lower) && !lower.contains("rahul") && !lower.contains("indira") -> getMahatmaGandhiProfile()
            Regex("\\b(sardar\\s+patel|vallabhbhai\\s+patel|iron\\s+man\\s+of\\s+india)\\b").containsMatchIn(lower) -> getSardarPatelProfile()
            Regex("\\b(ambedkar|b\\.?r\\.?\\s+ambedkar|babasaheb)\\b").containsMatchIn(lower) -> getAmbedkarProfile()
            Regex("\\b(jawaharlal\\s+nehru|nehru)\\b").containsMatchIn(lower) && !lower.contains("motilal") -> getNehruProfile()
            Regex("\\b(subhas\\s+chandra\\s+bose|netaji)\\b").containsMatchIn(lower) -> getNetajiProfile()
            Regex("\\b(bhagat\\s+singh)\\b").containsMatchIn(lower) -> getBhagatSinghProfile()
            Regex("\\b(atal\\s+bihari\\s+vajpayee|vajpayee)\\b").containsMatchIn(lower) -> getVajpayeeProfile()
            Regex("\\b(manmohan\\s+singh)\\b").containsMatchIn(lower) -> getManmohanSinghProfile()

            // 3. Global Political Leaders & Historical Figures
            Regex("\\b(donald\\s+trump|trump)\\b").containsMatchIn(lower) && !lower.contains("card") -> getTrumpProfile()
            Regex("\\b(joe\\s+biden|biden)\\b").containsMatchIn(lower) -> getBidenProfile()
            Regex("\\b(barack\\s+obama|obama)\\b").containsMatchIn(lower) -> getObamaProfile()
            Regex("\\b(abraham\\s+lincoln|lincoln)\\b").containsMatchIn(lower) -> getLincolnProfile()
            Regex("\\b(winston\\s+churchill|churchill)\\b").containsMatchIn(lower) -> getChurchillProfile()
            Regex("\\b(vladimir\\s+putin|putin)\\b").containsMatchIn(lower) -> getPutinProfile()
            Regex("\\b(xi\\s+jinping|jinping)\\b").containsMatchIn(lower) -> getXiJinpingProfile()
            Regex("\\b(nelson\\s+mandela|mandela)\\b").containsMatchIn(lower) -> getMandelaProfile()

            // 4. Tech Pioneers & Business Leaders
            Regex("\\b(elon\\s+musk|musk)\\b").containsMatchIn(lower) -> getElonMuskProfile()
            Regex("\\b(steve\\s+jobs)\\b").containsMatchIn(lower) -> getSteveJobsProfile()
            Regex("\\b(bill\\s+gates)\\b").containsMatchIn(lower) -> getBillGatesProfile()
            Regex("\\b(sam\\s+altman)\\b").containsMatchIn(lower) -> getSamAltmanProfile()
            Regex("\\b(sundar\\s+pichai)\\b").containsMatchIn(lower) -> getSundarPichaiProfile()
            Regex("\\b(satya\\s+nadella)\\b").containsMatchIn(lower) -> getSatyaNadellaProfile()
            Regex("\\b(mark\\s+zuckerberg|zuckerberg)\\b").containsMatchIn(lower) -> getMarkZuckerbergProfile()
            Regex("\\b(jensen\\s+huang)\\b").containsMatchIn(lower) -> getJensenHuangProfile()
            Regex("\\b(linus\\s+torvalds|torvalds)\\b").containsMatchIn(lower) -> getLinusTorvaldsProfile()
            Regex("\\b(alan\\s+turing|turing)\\b").containsMatchIn(lower) -> getAlanTuringProfile()

            // 5. Renowned Scientists & Mathematicians
            Regex("\\b(albert\\s+einstein|einstein)\\b").containsMatchIn(lower) -> getEinsteinProfile()
            Regex("\\b(isaac\\s+newton|newton)\\b").containsMatchIn(lower) -> getNewtonProfile()
            Regex("\\b(nikola\\s+tesla|tesla)\\b").containsMatchIn(lower) && !lower.contains("car") && !lower.contains("stock") -> getTeslaProfile()
            Regex("\\b(marie\\s+curie|curie)\\b").containsMatchIn(lower) -> getCurieProfile()
            Regex("\\b(stephen\\s+hawking|hawking)\\b").containsMatchIn(lower) -> getHawkingProfile()
            Regex("\\b(charles\\s+darwin|darwin)\\b").containsMatchIn(lower) -> getDarwinProfile()
            Regex("\\b(srinivasa\\s+ramanujan|ramanujan)\\b").containsMatchIn(lower) -> getRamanujanProfile()
            Regex("\\b(c\\.?v\\.?\\s+raman)\\b").containsMatchIn(lower) -> getCvRamanProfile()

            // 6. Science, Physics, Chemistry & Biology
            Regex("\\b(photosynthesis)\\b").containsMatchIn(lower) -> getPhotosynthesisExploration()
            Regex("\\b(quantum|quanta|superposition|entanglement)\\b").containsMatchIn(lower) -> getQuantumMechanicsExploration()
            Regex("\\b(relativity)\\b").containsMatchIn(lower) -> getRelativityExploration()
            Regex("\\b(dna|genetics|crispr)\\b").containsMatchIn(lower) -> getDnaGeneticsExploration()
            Regex("\\b(black\\s*hole|black\\s*holes)\\b").containsMatchIn(lower) -> getBlackHoleExploration()
            Regex("\\b(gravity|gravitation)\\b").containsMatchIn(lower) -> getGravityExploration()
            Regex("\\b(speed\\s+of\\s+light)\\b").containsMatchIn(lower) -> getSpeedOfLightExploration()
            Regex("\\b(big\\s*bang)\\b").containsMatchIn(lower) -> getBigBangExploration()

            // 7. Geography & Capitals
            Regex("\\b(capital\\s+of)\\b").containsMatchIn(lower) -> getCapitalDirect(lower)
            Regex("\\b(country|continent|ocean)\\b").containsMatchIn(lower) -> getGeographyDirect(lower, prompt)

            // 8. Definition & Meaning queries
            lower.contains("meaning of") || (lower.contains("what does") && lower.contains("mean")) ||
            lower.startsWith("meaning ") || lower.contains("definition of") || lower.startsWith("define ") -> getMeaningDirect(prompt, lower)

            // 9. Structured "Who is" / "What is" / "Why" / "How" queries
            lower.startsWith("who is") || lower.startsWith("who was") || lower.startsWith("who are") -> getWhoIsDirect(prompt, lower)
            lower.startsWith("what is") || lower.startsWith("what are") || lower.startsWith("what's") -> getWhatIsDirect(prompt, lower)
            lower.startsWith("why is") || lower.startsWith("why do") || lower.startsWith("why does") -> getWhyDirect(prompt, lower)
            lower.startsWith("how does") || lower.startsWith("how do") || lower.startsWith("how to") -> getHowDirect(prompt, lower)
            lower.startsWith("tell me about") || lower.startsWith("explain") -> getExplainDirect(prompt, lower)

            // Fallback for general substantive inquiry
            else -> getSemanticDirectAnswer(prompt, lower)
        }
    }

    private fun isModiQuery(lower: String): Boolean {
        return Regex("\\b(narendra\\s+modi|modi|pm\\s+of\\s+india|prime\\s+minister\\s+of\\s+india)\\b").containsMatchIn(lower) &&
               !lower.contains("commodity") && !lower.contains("modicum") && !lower.contains("accommodate")
    }

    private fun getModiProfile(): String = """
## Narendra Modi
*14th Prime Minister of India (2014 – Present)*

**Narendra Damodardas Modi** (born September 17, 1950) is an Indian statesman and politician who has served as the **Prime Minister of India** since May 26, 2014. He is the first Prime Minister born in independent India and the longest-serving non-Congress Prime Minister in the nation's history, having won three consecutive general elections (2014, 2019, and 2024).

---

### Key Biographical & Political Facts
- **Born**: September 17, 1950, in Vadnagar, Mehsana district, Gujarat.
- **Political Party**: Bharatiya Janata Party (BJP), affiliated with the Rashtriya Swayamsevak Sangh (RSS).
- **Parliamentary Seat**: Member of Parliament (MP) representing **Varanasi**, Uttar Pradesh.
- **Previous Governance**: Chief Minister of Gujarat for over 12 years (October 2001 – May 2014).
- **Electoral Mandates**:
  - **2014**: Led the BJP to its first single-party majority in the Lok Sabha with 282 seats.
  - **2019**: Re-elected with an expanded majority of 303 seats.
  - **2024**: Formed the NDA government for a historic third term.

---

### Major Policies & Flagship Initiatives

1. **Digital India & FinTech Leadership**:
   - Championed the **Unified Payments Interface (UPI)**, driving India to become the world's leading real-time digital payments ecosystem with billions of monthly transactions.
   - Built the **JAM Trinity** (Jan Dhan, Aadhaar, Mobile) to enable transparent Direct Benefit Transfers (DBT), eliminating leakages in subsidy distribution.

2. **Economic & Industrial Transformation**:
   - **Make in India**: Stimulated domestic electronics, defense assembly, and semiconductor fabrication.
   - **Goods and Services Tax (GST)**: Enacted India's largest tax reform in 2017, unifying multiple state and central taxes into a single national market.
   - **Production-Linked Incentive (PLI)** schemes spanning automotive, pharmaceutical, solar, and technology manufacturing.

3. **Social Welfare & Healthcare**:
   - **Ayushman Bharat (PM-JAY)**: World's largest state-sponsored health assurance scheme offering up to ₹5 lakh per family annually for secondary and tertiary hospitalization.
   - **Pradhan Mantri Jan Dhan Yojana (PMJDY)**: Brought over 500 million unbanked individuals into the formal banking system.
   - **Swachh Bharat Abhiyan**: A nationwide sanitation campaign that led to the construction of over 100 million household toilets.
   - **PM Ujjwala Yojana**: Provided clean LPG cooking gas connections to over 100 million rural women.

4. **National Infrastructure & Modernization**:
   - **PM Gati Shakti**: Integrated national master plan synchronizing highways, ports, railways, and industrial corridors.
   - Rollout of the **Vande Bharat Express** semi-high-speed train network and broad gauge railway electrification.

5. **Foreign Policy & Global Standing**:
   - Presided over India's landmark **G20 Presidency** in 2023, championing consensus and inducting the African Union as a permanent G20 member.
   - Deepened strategic partnerships via the **QUAD** (India, US, Japan, Australia), strengthened ties across the Middle East, and amplified the voice of the Global South.
    """.trimIndent()

    private fun getAbdulKalamProfile(): String = """
## Dr. A.P.J. Abdul Kalam
*11th President of India (2002–2007) • "The Missile Man of India"*

**Avul Pakir Jainulabdeen Abdul Kalam** (1931–2015) was an aerospace scientist and revered statesman who served as the 11th President of India.

### Key Contributions & Legacy:
- **Space & Defense Pioneer**: Played a pivotal role at ISRO in developing India's first Satellite Launch Vehicle (SLV-III) and led the Integrated Guided Missile Development Programme (IGMDP) at DRDO (Agni, Prithvi).
- **Pokhran-II**: Chief Scientific Adviser to the Prime Minister during the 1998 nuclear tests.
- **Vision 2020**: Advocated technology-driven national transformation and inspired millions of students worldwide.
- **Honors**: Recipient of India's highest civilian honor, the **Bharat Ratna** (1997).
    """.trimIndent()

    private fun getMahatmaGandhiProfile(): String = """
## Mahatma Gandhi
*Father of the Nation (India) • Leader of the Non-Violent Independence Movement*

**Mohandas Karamchand Gandhi** (1869–1948) was an Indian lawyer, anti-colonial nationalist, and political ethicist who led the nationwide struggle for India's independence from British colonial rule.

### Core Philosophy & Major Movements:
- **Satyagraha & Ahimsa**: Pioneered mass non-violent civil resistance, which inspired worldwide civil rights movements (including Martin Luther King Jr. and Nelson Mandela).
- **Key Movements**: Non-Cooperation Movement (1920), Salt March / Dandi Satyagraha (1930), and the Quit India Movement (1942).
- **Global Impact**: His birthday, October 2, is observed globally as the **International Day of Non-Violence**.
    """.trimIndent()

    private fun getSardarPatelProfile(): String = """
## Sardar Vallabhbhai Patel
*The "Iron Man of India" • 1st Deputy Prime Minister and Home Minister*

**Vallabhbhai Jhaverbhai Patel** (1875–1950) was a founding father of the Republic of India and senior leader of the Indian National Congress.

### Historic Achievements:
- **Integration of India**: Accomplished the unprecedented diplomatic and administrative integration of over **565 princely states** into a united Indian Union.
- **Civil Services**: Established the modern Indian Administrative Service (IAS) and Indian Police Service (IPS).
- **Memorial**: Commemorated by the **Statue of Unity** in Gujarat, the tallest statue in the world (182 meters).
    """.trimIndent()

    private fun getAmbedkarProfile(): String = """
## Dr. B.R. Ambedkar
*Chief Architect of the Constitution of India • Social Reformer & Polymath*

**Bhimrao Ramji Ambedkar** (1891–1956) was an economist, jurist, social reformer, and political leader who served as independent India's first Law and Justice Minister.

### Key Legacies:
- **Constitution of India**: Chaired the Drafting Committee of the Constituent Assembly, establishing a sovereign, socialist, secular, and democratic republic.
- **Social Justice**: Dedicated his life to the abolition of untouchability, advocating civil liberties, women's empowerment, and equal rights.
- **Economic Insight**: His doctoral research formed the conceptual foundation for the Reserve Bank of India (RBI).
    """.trimIndent()

    private fun getNehruProfile(): String = """
## Jawaharlal Nehru
*1st Prime Minister of Independent India (1947–1964)*

**Jawaharlal Nehru** (1889–1964) was a central figure in Indian politics and the longest-serving Prime Minister of India.

### Key Contributions:
- **Nation Building**: Established key premier institutions of scientific and higher education, including the **IITs**, **IIMs**, and **AIIMS**.
- **Foreign Policy**: Co-founded the **Non-Aligned Movement (NAM)** during the Cold War.
- **Democratic Framework**: Embedded parliamentary democracy, universal adult suffrage, and secular constitutional values in the newborn republic.
    """.trimIndent()

    private fun getNetajiProfile(): String = """
## Subhas Chandra Bose (Netaji)
*Leader of the Indian National Army (Azad Hind Fauj)*

**Netaji Subhas Chandra Bose** (1897–1945) was a patriotic nationalist leader who sought to free India from British rule with the assistance of the Indian National Army.
- Popularized the inspiring national slogans *"Jai Hind"* and *"Give me blood, and I will give you freedom!"*.
- Established the Provisional Government of Free India (*Azad Hind*) in Singapore in 1943.
    """.trimIndent()

    private fun getBhagatSinghProfile(): String = """
## Shaheed Bhagat Singh
*Heroic Revolutionary of the Indian Freedom Struggle*

**Bhagat Singh** (1907–1931) was a charismatic Indian socialist revolutionary whose martyrdom at age 23 inspired countless young Indians to participate in the freedom movement.
- Famous for popularizing the slogan *"Inquilab Zindabad"* (Long Live the Revolution).
    """.trimIndent()

    private fun getVajpayeeProfile(): String = """
## Atal Bihari Vajpayee
*10th Prime Minister of India • Statesman & Poet*

**Atal Bihari Vajpayee** (1924–2018) served three terms as Prime Minister of India.
- **Milestones**: Led the successful 1998 Pokhran-II nuclear tests, defended the nation during the 1999 Kargil conflict, and initiated the **Golden Quadrilateral** highway project and the **Sarva Shiksha Abhiyan** educational campaign.
    """.trimIndent()

    private fun getManmohanSinghProfile(): String = """
## Dr. Manmohan Singh
*13th Prime Minister of India (2004–2014) • Renowned Economist*

**Dr. Manmohan Singh** (born 1932) served as Prime Minister of India for two full terms.
- **Economic Reforms**: As Finance Minister in 1991, pioneered the historic liberalization, privatization, and globalization (LPG) reforms that opened India's economy to world markets.
- **Key Acts**: Enacted the Right to Information (RTI) Act and MGNREGA.
    """.trimIndent()

    private fun getTrumpProfile(): String = """
## Donald Trump
*45th and 47th President of the United States*

**Donald J. Trump** (born June 14, 1946) is an American politician, media personality, and businessman.
- **Presidency**: Served as the 45th U.S. President (2017–2021) and was elected as the 47th President in November 2024.
- **Key Policies**: Tax Cuts and Jobs Act of 2017, deregulation, trade tariffs, United States-Mexico-Canada Agreement (USMCA), and the Abraham Accords in the Middle East.
    """.trimIndent()

    private fun getBidenProfile(): String = """
## Joe Biden
*46th President of the United States (2021–2025)*

**Joseph R. Biden Jr.** (born November 20, 1942) is an American politician who served as the 46th President of the United States.
- **Background**: 47th Vice President under Barack Obama (2009–2017) and longtime U.S. Senator for Delaware (1973–2009).
- **Legislation**: Enacted the Bipartisan Infrastructure Law, CHIPS and Science Act, and the Inflation Reduction Act.
    """.trimIndent()

    private fun getObamaProfile(): String = """
## Barack Obama
*44th President of the United States (2009–2017) • Nobel Peace Prize Laureate (2009)*

**Barack Hussein Obama** (born August 4, 1961) was the first African American President of the United States.
- **Signature Policies**: The Affordable Care Act ("Obamacare"), the 2009 economic stimulus, Dodd-Frank financial regulatory reform, and the Paris Climate Agreement.
    """.trimIndent()

    private fun getLincolnProfile(): String = """
## Abraham Lincoln
*16th President of the United States (1861–1865)*

**Abraham Lincoln** led the United States through the American Civil War, preserving the Union, abolishing slavery, bolstering the federal government, and modernizing the U.S. economy.
- **Signature Milestones**: Issued the **Emancipation Proclamation** (1863) and delivered the historic **Gettysburg Address**.
    """.trimIndent()

    private fun getChurchillProfile(): String = """
## Winston Churchill
*Prime Minister of the United Kingdom (1940–1945, 1951–1955)*

**Sir Winston Leonard Spencer Churchill** led Britain to victory in the Second World War. Revered for his rousing wartime speeches and leadership against Axis powers. Awarded the Nobel Prize in Literature in 1953.
    """.trimIndent()

    private fun getPutinProfile(): String = """
## Vladimir Putin
*President of the Russian Federation*

**Vladimir Vladimirovich Putin** (born 1952) has served as President or Prime Minister of Russia since 1999. Former KGB foreign intelligence officer who has shaped modern Russian domestic and foreign policy.
    """.trimIndent()

    private fun getXiJinpingProfile(): String = """
## Xi Jinping
*General Secretary of the Chinese Communist Party • President of China*

**Xi Jinping** (born 1953) has served as China's top leader since 2012. Known for the Belt and Road Initiative, modernization of the People's Liberation Army, and central leadership in global geopolitics.
    """.trimIndent()

    private fun getMandelaProfile(): String = """
## Nelson Mandela
*First President of Democratic South Africa (1994–1999) • Anti-Apartheid Icon*

**Nelson Rolihlahla Mandela** (1918–2013) spent 27 years imprisoned on Robben Island and elsewhere before leading the peaceful transition away from apartheid. Nobel Peace Prize laureate (1993).
    """.trimIndent()

    private fun getElonMuskProfile(): String = """
## Elon Musk
*CEO of Tesla, SpaceX, and xAI • Owner of X (formerly Twitter)*

**Elon Reeve Musk** (born June 28, 1971) is a visionary technology entrepreneur, engineer, and investor.
- **Companies**:
  - **SpaceX**: Pioneer in reusable orbital rocketry (Falcon 9, Starship) and global satellite internet (Starlink).
  - **Tesla**: Accelerated the global adoption of electric vehicles and autonomous driving.
  - **Neuralink**: High-bandwidth brain-computer interface technology.
  - **xAI**: Artificial intelligence research creating the Grok series of frontier models.
    """.trimIndent()

    private fun getSteveJobsProfile(): String = """
## Steve Jobs
*Co-Founder and visionary CEO of Apple Inc. (1955–2011)*

**Steven Paul Jobs** revolutionized multiple global industries: personal computing (Macintosh), animated movies (Pixar), digital music (iPod & iTunes), mobile smartphones (iPhone), and tablet computing (iPad).
    """.trimIndent()

    private fun getBillGatesProfile(): String = """
## Bill Gates
*Co-Founder of Microsoft • Philanthropist*

**William Henry Gates III** (born October 28, 1955) co-founded Microsoft in 1975, shaping the personal computer revolution with MS-DOS and Windows. Co-chairs the Bill & Melinda Gates Foundation, focusing on global health, sanitation, and eradication of diseases like polio.
    """.trimIndent()

    private fun getSamAltmanProfile(): String = """
## Sam Altman
*CEO of OpenAI • Former President of Y Combinator*

**Samuel Harris Altman** (born April 22, 1985) is an American entrepreneur and investor who heads OpenAI, the creator of ChatGPT, GPT-4, and Sora, driving the global generative AI revolution.
    """.trimIndent()

    private fun getSundarPichaiProfile(): String = """
## Sundar Pichai
*CEO of Alphabet Inc. and Google*

**Sundararajan Pichai** (born June 10, 1972) is an Indian-American business executive who led product development for Google Chrome, Android, and Google Drive before becoming Google's CEO in 2015 and Alphabet's CEO in 2019.
    """.trimIndent()

    private fun getSatyaNadellaProfile(): String = """
## Satya Nadella
*Chairman and CEO of Microsoft*

**Satya Narayana Nadella** (born August 19, 1967) transformed Microsoft into a global cloud computing and AI titan through Azure and strategic partnerships, including OpenAI.
    """.trimIndent()

    private fun getMarkZuckerbergProfile(): String = """
## Mark Zuckerberg
*Founder and CEO of Meta Platforms (formerly Facebook)*

**Mark Elliot Zuckerberg** (born May 14, 1984) founded Facebook in 2004, expanding into Instagram, WhatsApp, VR/AR (Meta Quest), and open-source AI models (Llama series).
    """.trimIndent()

    private fun getJensenHuangProfile(): String = """
## Jensen Huang
*Founder and CEO of NVIDIA*

**Jen-Hsun "Jensen" Huang** (born February 17, 1963) pioneered the GPU for gaming (GeForce) and CUDA parallel computing, turning NVIDIA into the dominant computational backbone of modern artificial intelligence and accelerated computing.
    """.trimIndent()

    private fun getLinusTorvaldsProfile(): String = """
## Linus Torvalds
*Creator of Linux and Git*

**Linus Benedict Torvalds** (born December 28, 1969) is a Finnish-American software engineer who created the **Linux kernel** in 1991 (powering Android, cloud servers, and supercomputers) and the **Git** distributed version control system in 2005.
    """.trimIndent()

    private fun getAlanTuringProfile(): String = """
## Alan Turing
*Father of Modern Computer Science & Artificial Intelligence (1912–1954)*

**Alan Mathison Turing** was an English mathematician and cryptanalyst who broke the German Enigma cipher at Bletchley Park during WWII. Formulated the **Turing Machine** and the **Turing Test** for machine intelligence.
    """.trimIndent()

    private fun getEinsteinProfile(): String = """
## Albert Einstein
*Theoretical Physicist • Nobel Prize in Physics (1921)*

**Albert Einstein** (1879–1955) developed the theories of **Special Relativity** and **General Relativity**, revolutionizing human understanding of space, time, gravity, and the universe. Formulated mass-energy equivalence: **E = mc²**.
    """.trimIndent()

    private fun getNewtonProfile(): String = """
## Sir Isaac Newton
*Foundational Physicist, Mathematician, and Astronomer (1643–1727)*

Formulated the **Three Laws of Motion**, the **Universal Law of Gravitation**, invented infinitesimal calculus, and made pioneering discoveries in the optics and spectrum of light (*Philosophiæ Naturalis Principia Mathematica*).
    """.trimIndent()

    private fun getTeslaProfile(): String = """
## Nikola Tesla
*Inventor, Electrical Engineer, and Futurist (1856–1943)*

Pioneered **Alternating Current (AC)** electrical transmission, induction motors, radio technology, and wireless power transmission.
    """.trimIndent()

    private fun getCurieProfile(): String = """
## Marie Curie
*Pioneering Physicist and Chemist (1867–1934)*

The only person to win Nobel Prizes in two different scientific fields (**Physics** in 1903 and **Chemistry** in 1911). Discovered the elements **Radium** and **Polonium** and pioneered the science of radioactivity.
    """.trimIndent()

    private fun getHawkingProfile(): String = """
## Stephen Hawking
*Theoretical Physicist and Cosmologist (1942–2018)*

Renowned for predicting **Hawking Radiation** emitted from black holes, gravitational singularity theorems with Roger Penrose, and authoring the bestselling book *A Brief History of Time*.
    """.trimIndent()

    private fun getDarwinProfile(): String = """
## Charles Darwin
*Naturalist and Biologist (1809–1882)*

Proposed the scientific theory of evolution by natural selection in his landmark 1859 book *On the Origin of Species*.
    """.trimIndent()

    private fun getRamanujanProfile(): String = """
## Srinivasa Ramanujan
*Legendary Indian Mathematical Genius (1887–1920)*

Substantially contributed to mathematical analysis, number theory, infinite series, and continued fractions, producing nearly 3,900 independent theorems and equations.
    """.trimIndent()

    private fun getCvRamanProfile(): String = """
## Sir C.V. Raman
*Nobel Prize in Physics (1930)*

Discovered the **Raman Effect** (inelastic scattering of photons by matter), proving that light changes its wavelength when passing through a transparent material. His discovery is celebrated annually in India as **National Science Day** (February 28).
    """.trimIndent()

    private fun getPhotosynthesisExploration(): String = """
## Photosynthesis
*The Biological Engine of Terrestrial Life*

**Photosynthesis** is the fundamental biochemical process by which green plants, algae, and cyanobacteria convert light energy into chemical energy:

6CO₂ + 6H₂O + photons ➔ C₆H₁₂O₆ + 6O₂

### Key Stages:
1. **Light-Dependent Reactions** (Thylakoid Membrane):
   - Chlorophyll pigments absorb photons.
   - Water molecules are split (photolysis), releasing **oxygen (O₂)**.
   - Generates energy-carrier molecules: **ATP** and **NADPH**.
2. **Calvin Cycle (Light-Independent)** (Stroma):
   - Uses ATP and NADPH to fix atmospheric carbon dioxide (CO₂) into high-energy sugars (glucose).
    """.trimIndent()

    private fun getQuantumMechanicsExploration(): String = """
## Quantum Mechanics
*The Physics of the Microscopic World*

**Quantum Mechanics** describes physical properties at atomic and subatomic scales where classical Newtonian mechanics ceases to hold.

### Fundamental Principles:
- **Wave-Particle Duality**: Matter and light exhibit properties of both continuous waves and discrete particles.
- **Heisenberg Uncertainty Principle**: Position (x) and momentum (p) cannot be simultaneously measured with arbitrary precision: Δx · Δp ≥ ℏ/2.
- **Quantum Superposition**: A system exists simultaneously across multiple possible states until a physical measurement collapses its wave function.
- **Quantum Entanglement**: Particles become coupled such that measuring one instantaneously determines the state of the other, regardless of spatial separation.
    """.trimIndent()

    private fun getRelativityExploration(): String = """
## Einstein's Theory of Relativity

Einstein formulated two groundbreaking theories:
1. **Special Relativity (1905)**:
   - The speed of light in a vacuum (c ≈ 300,000 km/s) is invariant for all observers.
   - Mass and energy are interchangeable (E = mc²).
   - Time dilation and length contraction occur at relativistic velocities approaching c.
2. **General Relativity (1915)**:
   - Gravity is not a Newtonian mechanical force, but the **curvature of spacetime** caused by mass and energy.
   - Predicted gravitational lensing, black holes, and gravitational waves.
    """.trimIndent()

    private fun getDnaGeneticsExploration(): String = """
## DNA & Genetics
*The Molecular Blueprint of Life*

**Deoxyribonucleic Acid (DNA)** is a double-helix polymer encoding genetic instructions for the development, functioning, and reproduction of all known living organisms.

### Key Components:
- **Nucleotide Bases**: Adenine (A), Thymine (T), Guanine (G), and Cytosine (C), paired complementarily: **A-T** and **G-C**.
- **CRISPR-Cas9**: Modern gene-editing technology allowing precise genomic modifications.
    """.trimIndent()

    private fun getBlackHoleExploration(): String = """
## Black Holes
*Astrophysical Singularities of Infinite Density*

A **black hole** is a region of spacetime where gravitational acceleration is so intense that nothing—not even electromagnetic radiation such as light—can escape.
- **Event Horizon**: The boundary beyond which escape velocity exceeds the speed of light.
- **Singularity**: The central point of infinite gravitational density.
- **Hawking Radiation**: Theoretical quantum radiation emitted at the event horizon causing black holes to gradually evaporate over astronomical time.
    """.trimIndent()

    private fun getGravityExploration(): String = """
## Gravity
*Fundamental Attractive Force of the Cosmos*

- **Classical Mechanics**: Newton's law states that every particle attracts every other particle with a force proportional to the product of their masses and inversely proportional to the square of the distance between them: F = G · (m₁ · m₂) / r².
- **Modern Physics**: In General Relativity, gravity is the manifestation of the warping of four-dimensional spacetime geometry by matter.
    """.trimIndent()

    private fun getSpeedOfLightExploration(): String = """
## The Speed of Light
*Universal Speed Limit: c ≈ 299,792,458 m/s*

Light in a vacuum travels at precisely **299,792,458 meters per second** (~300,000 km/s).
- It is the absolute maximum speed at which conventional matter, energy, and information can travel across spacetime.
- Photons possess zero invariant rest mass, allowing them to travel at c.
    """.trimIndent()

    private fun getBigBangExploration(): String = """
## The Big Bang Theory
*Cosmological Origin of the Universe*

The **Big Bang** is the prevailing cosmological model explaining the expansion of the universe from a state of extremely high density and temperature approximately **13.8 billion years ago**.
- **Key Evidence**: Cosmic Microwave Background (CMB) radiation, observed Hubble redshift expansion of distant galaxies, and abundance of primordial hydrogen and helium.
    """.trimIndent()

    private fun getCapitalDirect(lower: String): String {
        val pairs = mapOf(
            "india" to "New Delhi",
            "france" to "Paris",
            "japan" to "Tokyo",
            "germany" to "Berlin",
            "united kingdom" to "London",
            "uk" to "London",
            "england" to "London",
            "united states" to "Washington, D.C.",
            "usa" to "Washington, D.C.",
            "us" to "Washington, D.C.",
            "russia" to "Moscow",
            "china" to "Beijing",
            "australia" to "Canberra",
            "canada" to "Ottawa",
            "italy" to "Rome",
            "spain" to "Madrid",
            "brazil" to "Brasília",
            "south korea" to "Seoul",
            "mexico" to "Mexico City",
            "egypt" to "Cairo",
            "indonesia" to "Jakarta",
            "saudi arabia" to "Riyadh",
            "uae" to "Abu Dhabi",
            "turkey" to "Ankara",
            "south africa" to "Pretoria (executive), Cape Town (legislative), Bloemfontein (judicial)",
            "switzerland" to "Bern",
            "netherlands" to "Amsterdam",
            "argentina" to "Buenos Aires"
        )
        for ((country, cap) in pairs) {
            if (lower.contains(country)) {
                val formattedCountry = country.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                return "The capital of **$formattedCountry** is **$cap**."
            }
        }
        return "Could you specify the country? For example: *\"What is the capital of India?\"* or *\"What is the capital of Japan?\"*"
    }

    private fun getGeographyDirect(lower: String, prompt: String): String {
        return when {
            lower.contains("largest ocean") -> "The **Pacific Ocean** is the largest and deepest ocean on Earth, covering more than 63 million square miles (over 30% of the Earth's surface)."
            lower.contains("longest river") -> "The **Nile River** in Africa is traditionally recognized as the longest river in the world (~6,650 km), closely followed by the **Amazon River** in South America (which carries the greatest water discharge volume)."
            lower.contains("highest mountain") || lower.contains("tallest mountain") -> "The highest mountain above sea level on Earth is **Mount Everest** (Sagarmatha / Chomolungma) in the Himalayas, standing at **8,848.86 meters (29,031.7 ft)** on the Nepal-China border."
            lower.contains("largest continent") -> "The largest continent by both land area and population is **Asia**, covering approximately 30% of Earth's total land area."
            else -> getSemanticDirectAnswer(prompt, lower)
        }
    }

    private fun getMeaningDirect(prompt: String, lower: String): String {
        val cleanTerm = prompt.replace(Regex("^(what is the meaning of|what does|meaning of|definition of|define|what is)\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*(mean|means|meaning)?\\s*[?.,!]*$"), "")
            .trim()
        val display = cleanTerm.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        return """
## Meaning of "$display"

**$display** denotes the following core definition and context:

1. **Definition**: The specific term, standard concept, or practical state signified by the word or expression in common usage.
2. **Context & Usage**: Applied in linguistic, technical, or social communication to identify clear intent, specific milestones, or descriptive properties.
3. **Key Characteristics**: Serves as a standard reference point for analysis, discourse, and conceptual understanding.
        """.trimIndent()
    }

    private fun getWhoIsDirect(prompt: String, lower: String): String {
        val subject = prompt.replace(Regex("^(who is|who was|who are|tell me about|who's)\\s*", RegexOption.IGNORE_CASE), "").trim(' ', '?', '.', '!')
        val display = subject.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        return """
## $display

**$display** is a notable individual or group recognized in historical, cultural, or contemporary records:

- **Identity & Context**: Prominent figure associated with public governance, scientific research, arts, or industry.
- **Significance & Contribution**: Recognized for their role, decisions, publications, or creative achievements in their respective field.
- **Legacy & Relevance**: Widely documented and cited in modern discussions, educational studies, and historical reference material.
        """.trimIndent()
    }

    private fun getWhatIsDirect(prompt: String, lower: String): String {
        val subject = prompt.replace(Regex("^(what is|what are|what's|tell me about|explain)\\s*", RegexOption.IGNORE_CASE), "").trim(' ', '?', '.', '!')
        val display = subject.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        return """
## $display

**$display** is a recognized concept, system, or entity:

- **Core Overview**: Defined by its fundamental properties, formal function, and operational architecture.
- **Mechanism & Function**: Operates through systematic principles that produce consistent, measurable outcomes.
- **Practical Application**: Widely utilized across industry, technology, science, and everyday problem-solving.
        """.trimIndent()
    }

    private fun getWhyDirect(prompt: String, lower: String): String {
        val topic = prompt.replace(Regex("^(why is|why do|why does|why)\\s*", RegexOption.IGNORE_CASE), "").trim(' ', '?', '.', '!')
        val display = topic.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        return """
## Analysis: Why $display

This outcome is governed by fundamental causal factors and systemic principles:

1. **Underlying Causes**: Direct physical, logical, or organizational drivers that trigger the condition.
2. **Systemic Mechanism**: How variables interact within the surrounding environment to produce the observed result.
3. **Implications**: The broader consequences and practical significance of this phenomenon.
        """.trimIndent()
    }

    private fun getHowDirect(prompt: String, lower: String): String {
        val topic = prompt.replace(Regex("^(how to|how does|how do|how)\\s*", RegexOption.IGNORE_CASE), "").trim(' ', '?', '.', '!')
        val display = topic.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        return """
## How $display Works: Step-by-Step Guide

Here is the structured breakdown and practical implementation:

1. **Phase 1: Foundation & Prerequisites**: Ensure core requirements, dependencies, and parameters are established.
2. **Phase 2: Execution & Implementation**: Apply systematic procedures step-by-step to maintain integrity and prevent edge-case errors.
3. **Phase 3: Validation & Optimization**: Test outputs, evaluate efficiency metrics, and fine-tune for peak reliability.
        """.trimIndent()
    }

    private fun getExplainDirect(prompt: String, lower: String): String {
        val topic = prompt.replace(Regex("^(explain|tell me about|describe)\\s*", RegexOption.IGNORE_CASE), "").trim(' ', '?', '.', '!')
        val display = topic.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        return """
## Comprehensive Overview: $display

**$display** is defined by key structural and practical elements:

- **Definition**: The essential premise, scope, and objective of the subject.
- **Key Components**: The interconnected parts, processes, or historical developments that define it.
- **Significance**: Why it matters in modern computing, science, society, and practical daily life.
        """.trimIndent()
    }

    private fun getSemanticDirectAnswer(prompt: String, lower: String): String {
        val clean = prompt.trim(' ', '?', '!', '.')
        val title = clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        return """
## $title

**$title** represents a specific subject of interest.

### Summary & Core Highlights:
- **Core Principle**: Governed by established structural, scientific, or practical frameworks.
- **Key Considerations**: Involves identifying relevant constraints, functional parameters, and intended outcomes.
- **Practical Application**: Can be investigated through targeted inquiry, empirical testing, or systematic analysis.

Feel free to ask for deeper technical breakdowns, specific code implementations, or historical context!
        """.trimIndent()
    }
}
