export interface ExploitReference {
  readonly source: string;
  readonly description: string | null;
  readonly url: string;
}

export interface CveEntry {
  readonly cveId: string;
  readonly description: string;
  readonly cvssScore: number | null;
  readonly severity: string | null;
  readonly cvssVector: string | null;
  readonly affectedVersions: string[];
  readonly exploits: ExploitReference[];
  readonly publishedDate: string | null;
  readonly lastModifiedDate: string | null;
}

export interface VulnerabilityLookupResponse {
  readonly serviceKey: string;
  readonly serviceName: string;
  readonly serviceVersion: string;
  readonly status: 'FETCHED' | 'NO_RESULTS';
  readonly totalCves: number;
  readonly fetchedAt: string;
  readonly cves: CveEntry[];
}

export interface VulnerabilityListItem {
  readonly serviceKey: string;
  readonly serviceName: string;
  readonly serviceVersion: string;
  readonly status: 'FETCHED' | 'NO_RESULTS';
  readonly totalCves: number;
  readonly maxSeverity: string | null;
  readonly fetchedAt: string;
}

export interface VulnerabilityListResponse {
  readonly content: VulnerabilityListItem[];
  readonly totalElements: number;
}
