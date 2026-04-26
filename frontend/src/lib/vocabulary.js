const FAVORITE_VOCABULARY_KEY = 'lep.favoriteVocabulary'

function safeParse(value) {
  try {
    return JSON.parse(value)
  } catch {
    return []
  }
}

export function normalizeVocabularyItem(item) {
  if (!item) return null
  const word = String(item.word || '').trim()
  if (!word) return null

  return {
    word,
    normalized: word.toLowerCase(),
    phonetic: item.phonetic || '',
    meaning: item.meaning || '',
    topic: item.topic || '会话词汇',
    savedAt: item.savedAt || new Date().toISOString()
  }
}

export function getFavoriteVocabulary() {
  if (typeof window === 'undefined') return []
  const raw = window.localStorage.getItem(FAVORITE_VOCABULARY_KEY)
  const parsed = safeParse(raw || '[]')
  return parsed
    .map(normalizeVocabularyItem)
    .filter(Boolean)
    .sort((left, right) => new Date(right.savedAt).getTime() - new Date(left.savedAt).getTime())
}

export function saveFavoriteVocabulary(item) {
  if (typeof window === 'undefined') return []
  const normalizedItem = normalizeVocabularyItem({
    ...item,
    savedAt: new Date().toISOString()
  })
  if (!normalizedItem) return getFavoriteVocabulary()

  const current = getFavoriteVocabulary().filter((entry) => entry.normalized !== normalizedItem.normalized)
  const next = [normalizedItem, ...current]
  window.localStorage.setItem(FAVORITE_VOCABULARY_KEY, JSON.stringify(next))
  return next
}

export function removeFavoriteVocabulary(word) {
  if (typeof window === 'undefined') return []
  const normalized = String(word || '').trim().toLowerCase()
  const next = getFavoriteVocabulary().filter((entry) => entry.normalized !== normalized)
  window.localStorage.setItem(FAVORITE_VOCABULARY_KEY, JSON.stringify(next))
  return next
}
