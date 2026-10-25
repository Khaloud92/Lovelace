export interface SearchResult {
  id: string;
  title: string;
  subtitle: string;
  type: string;
  route: string;
}

export interface SearchSuggestion {
  queryText: string;
  count: number;
}
