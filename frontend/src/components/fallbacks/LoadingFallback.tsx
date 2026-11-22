import { Loader } from '@mantine/core';
import { FullPageCenter } from '../ui/FullPageCenter';

export function LoadingFallback() {
  return (
    <FullPageCenter>
      <Loader color="blue" />
    </FullPageCenter>
  );
}
